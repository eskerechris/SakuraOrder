package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderFilter;
import me.chris.sakuraOrder.api.model.OrderSort;
import me.chris.sakuraOrder.api.persistence.OrderRepository;
import me.chris.sakuraOrder.api.services.order.OrderCacheService;
import me.chris.sakuraOrder.api.services.order.OrderHistoryService;
import me.chris.sakuraOrder.model.PlayerHistory;
import me.chris.sakuraOrder.util.OrderSearch;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HistoryService implements OrderHistoryService {

    private static final long IDLE_TTL_NANOS = TimeUnit.MINUTES.toNanos(15);
    private static final long EVICTION_PERIOD_TICKS = 20L * 60;

    private final OrderPlugin plugin;
    private final OrderRepository orderRepository;
    private final OrderCacheService cacheService;

    /** Per-buyer closed orders (lifecycle over). Filled from the DB once, then kept fresh by close events. */
    private final Map<UUID, PlayerHistory> histories = new ConcurrentHashMap<>();

    private WrappedTask wrappedTask;

    public HistoryService(@NotNull OrderPlugin plugin, @NotNull OrderCacheService cacheService) {
        this.plugin = plugin;
        this.orderRepository = plugin.getOrderRepository();
        this.cacheService = cacheService;
        // Called synchronously by the cache, inside the atomic step that closes the order.
        // recordClosed must stay fast and must never touch the cache's mutation methods.
        cacheService.onOrderClosed(this::recordClosed);
    }

    /**
     * Idempotent. Also invoked automatically the first time a history is loaded, so eviction
     * works even if nobody calls it explicitly at plugin enable.
     */
    @Override
    public synchronized void start() {
        if (wrappedTask != null && !wrappedTask.isCancelled()) {
            return;
        }
        this.wrappedTask = SakuraOrder.getInstance().getSchedulerService().runTimerAsync(
                this::evictIdle,
                EVICTION_PERIOD_TICKS,
                EVICTION_PERIOD_TICKS
        );
    }

    @Override
    public synchronized void stop() {
        if (wrappedTask != null && !wrappedTask.isCancelled()) {
            wrappedTask.cancel();
        }
        wrappedTask = null;
    }

    @Override
    @NotNull
    public CompletableFuture<List<IOrder>> getPlayerHistory(
            @NotNull UUID buyerId,
            @NotNull OrderFilter filter,
            @NotNull OrderSort sort,
            @NotNull String search
    ) {
        PlayerHistory history = historyFor(buyerId);
        // Once the first load is done, ready() is already complete: thenApply runs synchronously
        // on the caller and there is no DB access for filter/sort/search changes.
        return history.ready().thenApply(v -> query(buyerId, history, filter, sort, search));
    }

    @Override
    public void invalidate(@NotNull UUID buyerId) {
        histories.remove(buyerId);
    }

    @NotNull
    private PlayerHistory historyFor(@NotNull UUID buyerId) {
        PlayerHistory cached = histories.get(buyerId);
        if (cached != null) {
            cached.touch();
            return cached;
        }

        PlayerHistory fresh = new PlayerHistory();
        PlayerHistory raced = histories.putIfAbsent(buyerId, fresh);
        if (raced != null) {
            raced.touch();
            return raced;
        }

        start(); // no-op when the eviction task is already running
        startLoad(buyerId, fresh);
        return fresh;
    }

    /**
     * The history is registered before this method runs, so newly closed orders are
     * captured by {@link #recordClosed(IOrder)}. A flush is required because earlier
     * closed orders may still have pending persistence operations.
     */
    private void startLoad(@NotNull UUID buyerId, @NotNull PlayerHistory history) {
        cacheService.flush()
                .thenCompose(v -> orderRepository.findByBuyerId(buyerId)
                        .thenAcceptBoth(
                                orderRepository.findOrderHistory(buyerId, 0, Integer.MAX_VALUE),
                                (current, archived) -> {
                                    history.addLoaded(current);
                                    history.addLoaded(archived);
                                }))
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        histories.remove(buyerId, history); // next call retries from scratch
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to load order history for buyer " + buyerId, ex);
                        history.markFailed(ex);
                    } else {
                        history.markReady();
                    }
                });
    }

    @NotNull
    private List<IOrder> query(
            @NotNull UUID buyerId,
            @NotNull PlayerHistory history,
            @NotNull OrderFilter filter,
            @NotNull OrderSort sort,
            @NotNull String search
    ) {
        history.touch();

        Map<UUID, IOrder> ordersById = new HashMap<>();
        // Read the live cache first and closed history second. Orders closing during the
        // read are therefore present in at least one source, if present in both, the
        // closed copy overwrites the live one.
        for (IOrder live : cacheService.getActiveByBuyer(buyerId)) {
            ordersById.put(live.getId(), live);
        }
        for (IOrder done : history.closedOrders()) {
            ordersById.put(done.getId(), done);
        }

        return ordersById.values().stream()
                .filter(filter.predicate())
                .filter(order -> OrderSearch.matches(order, search))
                .sorted(sort.comparator().thenComparing(IOrder::getId)) // deterministic ties -> stable paging
                .toList();
    }

    private void recordClosed(@NotNull IOrder closed) {
        PlayerHistory history = histories.get(closed.getBuyerId());
        if (history != null) {
            history.record(closed);
        }
    }

    private void evictIdle() {
        histories.values().removeIf(h -> h.isReady() && h.isIdleFor(IDLE_TTL_NANOS));
    }
}