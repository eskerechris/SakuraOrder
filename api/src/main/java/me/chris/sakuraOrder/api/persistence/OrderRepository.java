package me.chris.sakuraOrder.api.persistence;

import me.chris.sakuraOrder.api.model.IOrder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for persisting and querying orders.
 *
 * <p>Provides order-specific database operations in addition to the
 * generic persistence operations inherited from {@link Repository}.</p>
 */
public interface OrderRepository extends Repository<IOrder, UUID> {

    /**
     * Retrieves all non-terminal orders.
     *
     * <p>This includes orders with {@code ACTIVE} and {@code FULFILLED} status
     * and is primarily used to populate the in-memory order cache during startup.</p>
     *
     * @return a future containing all non-terminal orders
     */
    @NotNull
    CompletableFuture<List<IOrder>> findActiveOrders();

    /**
     * Retrieves all active orders created by the specified player.
     *
     * @param buyerId the unique identifier of the player who created the orders
     * @return a future containing the player's active orders
     */
    @NotNull
    CompletableFuture<List<IOrder>> findByBuyerId(@NotNull UUID buyerId);

    /**
     * Retrieves a paginated history of archived orders for a player.
     *
     * <p>Results are ordered by archive date in descending order, with the
     * most recently archived orders returned first.</p>
     *
     * @param buyerId the unique identifier of the player whose history should be retrieved
     * @param page the zero-based page index
     * @param pageSize the maximum number of orders to return
     * @return a future containing the requested page of archived orders
     */
    @NotNull
    CompletableFuture<List<IOrder>> findOrderHistory(@NotNull UUID buyerId, int page, int pageSize);

    /**
     * Archives a batch of terminal orders in a single atomic transaction.
     *
     * <p>Orders with {@code COMPLETED}, {@code CANCELLED}, or {@code EXPIRED}
     * status are moved from the active orders table to the order history table.</p>
     *
     * @param limit the maximum number of orders to archive in this execution
     * @return a future containing the number of orders successfully archived
     */
    @NotNull
    CompletableFuture<Integer> archiveCompletedOrders(int limit);
}