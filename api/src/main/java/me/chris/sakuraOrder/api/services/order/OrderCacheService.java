package me.chris.sakuraOrder.api.services.order;

import me.chris.sakuraOrder.api.model.CollectionAllocation;
import me.chris.sakuraOrder.api.model.DeliveryAllocation;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderFilter;
import me.chris.sakuraOrder.api.model.OrderSort;
import me.chris.sakuraOrder.api.model.OrderStatus;
import me.chris.sakuraOrder.api.model.OrderTransition;
import me.chris.sakuraOrder.api.persistence.OrderRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Provides an in-memory cache for non-terminal orders ({@link OrderStatus#ACTIVE}
 * and {@link OrderStatus#FULFILLED}).
 *
 * <p>The cache is initially populated from the {@link OrderRepository} and
 * persists changes and terminal transitions through the repository.</p>
 *
 * <p>Operations that modify an order are performed atomically against the
 * current cached state to prevent concurrent updates from overwriting each
 * other.</p>
 */
public interface OrderCacheService {

    /**
     * Loads all non-terminal orders from the repository into the cache.
     *
     * @return a future completed when the initial cache population has finished
     */
    @NotNull
    CompletableFuture<Void> loadActiveOrders();

    /**
     * @return a list with the expired orders
     */
    @NotNull
    List<IOrder> getExpired();

    /**
     * Waits for all queued order writes to complete.
     *
     * <p>This is used during plugin shutdown before the database connection
     * pool is closed, and as a synchronization barrier before reading order
     * data from the database to ensure that recent writes are visible.</p>
     *
     * @return a future completed when all pending writes have settled
     */
    @NotNull
    CompletableFuture<Void> flush();

    /**
     * Retrieves an order from the cache.
     *
     * @param orderId the unique identifier of the order
     * @return the cached order, or {@code null} if the order is not cached
     */
    @Nullable
    IOrder get(@NotNull UUID orderId);

    /**
     * Inserts or updates an order in the cache.
     *
     * <p>Orders with {@link OrderStatus#ACTIVE} or {@link OrderStatus#FULFILLED}
     * status are stored in the cache. Orders with terminal statuses such as
     * {@link OrderStatus#COMPLETED}, {@link OrderStatus#CANCELLED}, or
     * {@link OrderStatus#EXPIRED} are removed from the cache.</p>
     *
     * @param order the order to insert or update
     */
    void put(@NotNull IOrder order);

    /**
     * Transitions an order to {@link OrderStatus#FULFILLED}.
     *
     * <p>The new expiration timestamp is calculated using the configured
     * claim-grace period. The transition is applied atomically against the
     * current cached order and persisted through the repository.</p>
     *
     * <p>The live cached entry is used instead of the supplied snapshot to
     * prevent a concurrent {@link #reserveCollection} operation from being
     * overwritten.</p>
     *
     * @param order the order to mark as fulfilled
     */
    void fulfill(@NotNull IOrder order);

    /**
     * Removes an order from the cache.
     *
     * @param orderId the unique identifier of the order to remove
     */
    void remove(@NotNull UUID orderId);

    /**
     * Retrieves all cached non-terminal orders belonging to a specific buyer.
     *
     * <p>Both {@link OrderStatus#ACTIVE} and {@link OrderStatus#FULFILLED}
     * orders are included.</p>
     *
     * @param buyerId the unique identifier of the buyer
     * @return the buyer's cached non-terminal orders
     */
    @NotNull
    List<IOrder> getActiveByBuyer(@NotNull UUID buyerId);

    /**
     * Retrieves all cached non-terminal orders belonging to a specific buyer.
     *
     * <p>This is a semantic alias for {@link #getActiveByBuyer(UUID)} that
     * emphasizes that fulfilled orders awaiting collection are also included.</p>
     *
     * @param buyerId the unique identifier of the buyer
     * @return the buyer's cached non-terminal orders
     */
    @NotNull
    List<IOrder> getCachedByBuyer(@NotNull UUID buyerId);

    /**
     * Registers a listener that is invoked when an order leaves the live cache
     * with a terminal status.
     *
     * <p>The listener is invoked synchronously during the atomic cache
     * transition, after the persistence operation has been queued and before
     * the order is removed from the cache.</p>
     *
     * <p>Listeners must execute quickly, must not perform blocking operations,
     * and must not invoke mutating methods on this cache service. Exceptions
     * thrown by listeners are logged and ignored.</p>
     *
     * @param listener the listener to invoke when an order is closed
     */
    void onOrderClosed(@NotNull Consumer<IOrder> listener);

    /**
     * Applies a domain transition atomically to the current cached order.
     *
     * <p>The transition is evaluated against the live cached entry. If it
     * produces a different order instance, the resulting state is persisted
     * through the ordered write queue and stored in the cache. If the resulting
     * status is not live ({@link OrderStatus#ACTIVE} or
     * {@link OrderStatus#FULFILLED}), the order is removed from the cache and
     * registered close listeners are notified.</p>
     *
     * <p>Returning the same order instance indicates that no state change was
     * made, in which case nothing is persisted.</p>
     *
     * <p>This method is the extension point for services that own domain
     * rules but require the cache's atomicity guarantees. The supplied
     * {@link OrderTransition} must be fast, side-effect free, and must not
     * call back into the cache.</p>
     *
     * @param orderId the unique identifier of the order to transition
     * @param transition the domain rule to apply to the live order
     * @param <R> the type of value returned by the transition
     * @return the outcome produced by the transition, or {@code null} if the
     *         order is not present in the live cache
     */
    @Nullable
    <R> OrderTransition.Outcome<R> transition(
            @NotNull UUID orderId,
            @NotNull OrderTransition<R> transition
    );

    /**
     * Atomically reserves a portion of an order's remaining deliverable amount.
     *
     * <p>The reservation checks the current residual amount and increments
     * the delivered count by at most that amount. The operation is performed
     * atomically against the cached entry, preventing lost updates when
     * multiple deliveries target the same order concurrently.</p>
     *
     * @param orderId the unique identifier of the order to deliver against
     * @param providedAmount the amount offered by the deliverer
     * @return the resulting allocation, or {@code null} if no active order
     *         with the specified identifier exists
     */
    @Nullable
    DeliveryAllocation reserveDelivery(@NotNull UUID orderId, int providedAmount);

    /**
     * Atomically reserves a portion of an order's collectable amount.
     *
     * <p>The collectable amount is calculated as the delivered amount minus
     * the amount already collected. The collected count is then increased by
     * at most the requested amount in a single atomic operation, preventing
     * lost updates when collection and delivery occur concurrently.</p>
     *
     * <p>Collection is independent of whether the order is still
     * {@link OrderStatus#ACTIVE}. Partially delivered items may therefore be
     * collected immediately without preventing further deliveries.</p>
     *
     * <p>Once an order is {@link OrderStatus#FULFILLED} and all delivered
     * items have been collected, the order transitions to
     * {@link OrderStatus#COMPLETED} and is removed from the active cache.</p>
     *
     * @param orderId the unique identifier of the order to collect from
     * @param requestedAmount the amount requested by the collector
     * @return the resulting allocation, or {@code null} if no active order
     *         with the specified identifier exists
     */
    @Nullable
    CollectionAllocation reserveCollection(@NotNull UUID orderId, int requestedAmount);

    /**
     * Returns a page of open orders across all buyers.
     *
     * <p>Only {@link OrderStatus#ACTIVE} orders are included. Results are
     * ordered by creation time in ascending order.</p>
     *
     * @param page the zero-based page number
     * @param pageSize the maximum number of orders per page
     * @return the requested page of open orders
     */
    @NotNull
    List<IOrder> getPage(int page, int pageSize);

    /**
     * Returns a page of open orders filtered and sorted according
     * to the supplied criteria.
     *
     * <p>Only {@link OrderStatus#ACTIVE} orders are included.</p>
     *
     * @param page the zero-based page number
     * @param pageSize the maximum number of orders per page
     * @param filter the order filter to apply
     * @param sort the order sort to apply
     * @return the requested page of filtered and sorted orders
     */
    @NotNull
    List<IOrder> getPage(
            int page,
            int pageSize,
            @NotNull OrderFilter filter,
            @NotNull OrderSort sort
    );

    /**
     * Returns a page of open orders matching the supplied filter,
     * sort criteria, and item search.
     *
     * <p>Only {@link OrderStatus#ACTIVE} orders are included. The search is
     * matched against the item's material name using a case-insensitive
     * substring comparison. A blank search matches all orders.</p>
     *
     * @param page the zero-based page number
     * @param pageSize the maximum number of orders per page
     * @param filter the order filter to apply
     * @param sort the order sort to apply
     * @param search the free-text item search
     * @return the requested page of matching orders
     */
    @NotNull
    List<IOrder> getPage(
            int page,
            int pageSize,
            @NotNull OrderFilter filter,
            @NotNull OrderSort sort,
            @NotNull String search
    );

    /**
     * Returns the number of open orders currently cached.
     *
     * <p>Only {@link OrderStatus#ACTIVE} orders are counted.</p>
     *
     * @return the number of cached active orders
     */
    int getActiveCount();

    /**
     * Returns the number of open orders matching the supplied filter.
     *
     * <p>Only {@link OrderStatus#ACTIVE} orders are counted.</p>
     *
     * @param filter the order filter to apply
     * @return the number of matching active orders
     */
    int getActiveCount(@NotNull OrderFilter filter);

    /**
     * Returns the number of open orders matching the supplied filter and item search.
     *
     * <p>Only {@link OrderStatus#ACTIVE} orders are counted. The search is
     * matched against the item's material name using a case-insensitive
     * substring comparison. A blank search matches all orders.</p>
     *
     * @param filter the order filter to apply
     * @param search the free-text item search
     * @return the number of matching active orders
     */
    int getActiveCount(@NotNull OrderFilter filter, @NotNull String search);
}