package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.*;
import me.chris.sakuraOrder.api.persistence.OrderRepository;
import me.chris.sakuraOrder.api.services.order.OrderCacheService;
import me.chris.sakuraOrder.persistence.OrderPersistenceQueue;
import me.chris.sakuraOrder.util.OrderSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

import static me.chris.sakuraOrder.util.OrderCopies.*;

public class CacheService implements OrderCacheService {

    private final OrderPlugin plugin;
    private final OrderRepository orderRepository;
    private final Map<UUID, IOrder> activeOrders = new ConcurrentHashMap<>();
    private final OrderPersistenceQueue persistenceQueue = new OrderPersistenceQueue();
    private final List<Consumer<IOrder>> closeListeners = new CopyOnWriteArrayList<>();

    public CacheService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        this.orderRepository = plugin.getOrderRepository();
    }

    @Override
    @NotNull
    public CompletableFuture<Void> loadActiveOrders() {
        return orderRepository.findActiveOrders()
                .thenAccept(orders -> orders.forEach(o -> activeOrders.put(o.getId(), o)))
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load active orders into cache", ex);
                    throw new CompletionException(ex);
                });
    }

    @Override
    public void onOrderClosed(@NotNull Consumer<IOrder> listener) {
        closeListeners.add(listener);
    }

    /**
     * Notifies listeners that an order has been removed from the live cache with a
     * terminal status such as {@code COMPLETED} or {@code EXPIRED}.
     *
     * <p>Must be called after the persistence operation has been enqueued and before
     * the cache removal operation completes. Listener failures are logged and
     * swallowed to prevent them from affecting the cache update.
     */
    private void notifyClosed(@NotNull IOrder completed) {
        for (Consumer<IOrder> listener : closeListeners) {
            try {
                listener.accept(completed);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "Order close listener failed for order " + completed.getId(), ex);
            }
        }
    }

    @Override
    @Nullable
    public IOrder get(@NotNull UUID orderId) {
        return activeOrders.get(orderId);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> flush() {
        return persistenceQueue.flush();
    }

    @Override
    public void put(@NotNull IOrder order) {
        if (order.getOrderStatus() == OrderStatus.ACTIVE
                || order.getOrderStatus() == OrderStatus.FULFILLED) {
            activeOrders.put(order.getId(), order);
        } else {
            activeOrders.remove(order.getId());
            notifyClosed(order);
        }
    }

    @Override
    public void fulfill(@NotNull IOrder order) {
        int graceDays = plugin.getSettingsService().getGeneral().getClaimGraceDays();
        Instant newExpiresAt = Instant.now().plus(graceDays, ChronoUnit.DAYS);

        AtomicReference<IOrder> resultHolder = new AtomicReference<>();
        // Enqueued inside the computeIfPresent lambda, while this order's bucket lock is
        // held, so the OrderPersistenceQueue write order always matches the true mutation
        AtomicReference<CompletableFuture<Void>> saveFuture = new AtomicReference<>();

        activeOrders.computeIfPresent(order.getId(), (id, current) -> {
            if (current.getOrderStatus() == OrderStatus.COMPLETED
                    || current.getOrderStatus() == OrderStatus.CANCELLED
                    || current.getOrderStatus() == OrderStatus.EXPIRED) {
                resultHolder.set(current);
                return current; // terminal state: never regress, nothing to persist
            }

            IOrder withFulfilled = withStatusAndExpiry(current, OrderStatus.FULFILLED, newExpiresAt);

            // Handle the case where all items were collected before this transition to
            // FULFILLED. Otherwise, the order could remain stuck in FULFILLED because
            // reserveCollection() cannot complete the order until it is already FULFILLED.
            IOrder toPersist;
            IOrder newCacheValue;
            if (withFulfilled.getCollected() >= withFulfilled.getAmount()) {
                toPersist = withStatus(withFulfilled, OrderStatus.COMPLETED);
                newCacheValue = null; // atomic cache remove
            } else {
                toPersist = withFulfilled;
                newCacheValue = withFulfilled;
            }

            resultHolder.set(toPersist);
            saveFuture.set(persistenceQueue.enqueue(id, () -> orderRepository.save(toPersist)));
            if (newCacheValue == null) {
                notifyClosed(toPersist); // after the enqueue, before the removal
            }
            return newCacheValue;
        });

        IOrder result = resultHolder.get();
        if (result == null) {
            plugin.getLogger().warning("fulfill() called for order " + order.getId()
                    + " but it is no longer present in the active cache (removed concurrently); skipping.");
            return;
        }

        CompletableFuture<Void> future = saveFuture.get();
        if (future != null) {
            future.exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to persist " + result.getOrderStatus()
                        + " state for order " + result.getId(), ex);
                return null;
            });
        }
    }

    @Override
    public void remove(@NotNull UUID orderId) {
        activeOrders.remove(orderId);
    }

    @Override
    @NotNull
    public List<IOrder> getActiveByBuyer(@NotNull UUID buyerId) {
        return activeOrders.values().stream()
                .filter(o -> o.getBuyerId().equals(buyerId))
                .toList();
    }

    @Override
    @NotNull
    public List<IOrder> getCachedByBuyer(@NotNull UUID buyerId) {
        return getActiveByBuyer(buyerId);
    }

    @Override
    @NotNull
    public List<IOrder> getExpired() {
        return activeOrders.values().stream()
                .filter(IOrder::isExpired)
                .toList();
    }

    @Override
    @Nullable
    public DeliveryAllocation reserveDelivery(@NotNull UUID orderId, int providedAmount) {
        if (providedAmount <= 0) {
            IOrder existing = activeOrders.get(orderId);
            return existing != null ? new DeliveryAllocation(existing, 0) : null;
        }

        AtomicInteger reserved = new AtomicInteger(-1);

        IOrder updated = activeOrders.compute(orderId, (id, current) -> {
            if (current == null || current.getOrderStatus() != OrderStatus.ACTIVE) {
                return current; // order not found or full
            }

            long residual = Math.max(0L, current.getAmount() - current.getDelivered());
            int deliveredNow = (int) Math.min(providedAmount, residual);
            reserved.set(deliveredNow);

            if (deliveredNow == 0) {
                return current; // order full, nothing changed, nothing to persist
            }

            IOrder withDelivered = withDelivered(current, current.getDelivered() + deliveredNow);
            persistenceQueue.enqueue(id, () -> orderRepository.save(withDelivered));
            return withDelivered;
        });

        if (updated == null) return null; // order not found
        return new DeliveryAllocation(updated, Math.max(0, reserved.get()));
    }

    @Override
    @Nullable
    public CollectionAllocation reserveCollection(@NotNull UUID orderId, int requestedAmount) {
        if (requestedAmount <= 0) {
            IOrder existing = activeOrders.get(orderId);
            return existing != null ? new CollectionAllocation(existing, 0) : null;
        }

        AtomicInteger reserved = new AtomicInteger(-1);

        // Capture the final state here because COMPLETED returns null from compute()
        // to remove the order atomically, without a separate remove() operation.
        AtomicReference<IOrder> resultHolder = new AtomicReference<>();

        activeOrders.compute(orderId, (id, current) -> {
            if (current == null) {
                return null; // order not found (fulfilled/expired)
            }

            long collectable = Math.max(0L, current.getDelivered() - current.getCollected());
            int collectedNow = (int) Math.min(requestedAmount, collectable);
            reserved.set(collectedNow);

            if (collectedNow == 0) {
                resultHolder.set(current);
                return current; // Nothing is collectible at this time, nothing to persist
            }

            IOrder withNewCollected = withCollected(current, current.getCollected() + collectedNow);

            if (withNewCollected.getOrderStatus() == OrderStatus.FULFILLED
                    && withNewCollected.getCollected() >= withNewCollected.getAmount()) {
                IOrder completedOrder = withStatus(withNewCollected, OrderStatus.COMPLETED);
                resultHolder.set(completedOrder);
                persistenceQueue.enqueue(id, () -> orderRepository.save(completedOrder));
                notifyClosed(completedOrder); // after the enqueue, before the removal
                return null; // Atomic removal from the cache within the same compute()
            }

            resultHolder.set(withNewCollected);
            persistenceQueue.enqueue(id, () -> orderRepository.save(withNewCollected));
            return withNewCollected;
        });

        IOrder updated = resultHolder.get();
        if (updated == null) return null; // order not found

        return new CollectionAllocation(updated, Math.max(0, reserved.get()));
    }

    @Override
    @Nullable
    public <R> OrderTransition.Outcome<R> transition(@NotNull UUID orderId, @NotNull OrderTransition<R> transition) {
        AtomicReference<OrderTransition.Outcome<R>> holder = new AtomicReference<>();

        // Apply, persist, and update the cache atomically for this order. Persistence is
        // enqueued inside the lambda to preserve mutation order, and close listeners run
        // before the order is removed from the cache.
        activeOrders.computeIfPresent(orderId, (id, current) -> {
            OrderTransition.Outcome<R> outcome = transition.apply(current);
            holder.set(outcome);

            IOrder next = outcome.order();
            if (next == current) {
                return current; // nothing changed, nothing to persist
            }

            persistenceQueue.enqueue(id, () -> orderRepository.save(next));

            if (isLive(next)) {
                return next;
            }
            notifyClosed(next); // after the enqueue, before the removal
            return null; // atomic cache removal
        });

        return holder.get(); // null when the order is not in the live cache
    }

    private static boolean isLive(@NotNull IOrder order) {
        return order.getOrderStatus() == OrderStatus.ACTIVE
                || order.getOrderStatus() == OrderStatus.FULFILLED;
    }

    @Override
    @NotNull
    public List<IOrder> getPage(int page, int pageSize) {
        if (page < 0 || pageSize <= 0) return List.of();
        return activeOrders.values().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACTIVE)
                .sorted(Comparator.comparing(IOrder::getCreatedAt).thenComparing(IOrder::getId))
                .skip((long) page * pageSize)
                .limit(pageSize)
                .toList();
    }

    @Override
    @NotNull
    public List<IOrder> getPage(int page, int pageSize, @NotNull OrderFilter filter, @NotNull OrderSort sort) {
        if (page < 0 || pageSize <= 0) return List.of();
        return activeOrders.values().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACTIVE)
                .filter(filter.predicate())
                .sorted(sort.comparator().thenComparing(IOrder::getId)) // deterministic ties -> stable paging
                .skip((long) page * pageSize)
                .limit(pageSize)
                .toList();
    }

    @Override
    @NotNull
    public List<IOrder> getPage(int page, int pageSize, @NotNull OrderFilter filter, @NotNull OrderSort sort, @NotNull String search) {
        if (page < 0 || pageSize <= 0) return List.of();
        return activeOrders.values().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACTIVE)
                .filter(filter.predicate())
                .filter(o -> OrderSearch.matches(o, search))
                .sorted(sort.comparator().thenComparing(IOrder::getId)) // deterministic ties -> stable paging
                .skip((long) page * pageSize)
                .limit(pageSize)
                .toList();
    }

    @Override
    public int getActiveCount() {
        return (int) activeOrders.values().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACTIVE)
                .count();
    }

    @Override
    public int getActiveCount(@NotNull OrderFilter filter) {
        return (int) activeOrders.values().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACTIVE)
                .filter(filter.predicate())
                .count();
    }

    @Override
    public int getActiveCount(@NotNull OrderFilter filter, @NotNull String search) {
        return (int) activeOrders.values().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.ACTIVE)
                .filter(filter.predicate())
                .filter(o -> OrderSearch.matches(o, search))
                .count();
    }
}