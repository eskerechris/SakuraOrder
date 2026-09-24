package me.chris.sakuraOrder.model;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the closed order history of a single buyer outside the live order cache.
 */
public final class PlayerHistory {

    private final Map<UUID, IOrder> closed = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> ready = new CompletableFuture<>();
    private volatile long lastAccessNanos = System.nanoTime();

    /**
     * Returns the future completed when the initial database load finishes.
     *
     * <p>The future completes exceptionally if the initial load fails.</p>
     *
     * @return the history loading future
     */
    @NotNull
    public CompletableFuture<Void> ready() {
        return ready;
    }

    public boolean isReady() {
        return ready.isDone();
    }

    public void markReady() {
        ready.complete(null);
    }

    public void markFailed(@NotNull Throwable error) {
        ready.completeExceptionally(error);
    }

    /**
     * Adds closed orders loaded from persistent storage without overwriting
     * orders already recorded while the load was in progress.
     */
    public void addLoaded(@NotNull List<IOrder> orders) {
        for (IOrder order : orders) {
            if (!isLive(order)) {
                closed.putIfAbsent(order.getId(), order);
            }
        }
    }

    public void record(@NotNull IOrder order) {
        closed.put(order.getId(), order);
    }

    @NotNull
    public Collection<IOrder> closedOrders() {
        return Collections.unmodifiableCollection(closed.values());
    }

    private static boolean isLive(@NotNull IOrder order) {
        OrderStatus status = order.getOrderStatus();
        return status == OrderStatus.ACTIVE || status == OrderStatus.FULFILLED;
    }

    public void touch() {
        lastAccessNanos = System.nanoTime();
    }

    public boolean isIdleFor(long ttlNanos) {
        return System.nanoTime() - lastAccessNanos > ttlNanos;
    }
}