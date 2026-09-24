package me.chris.sakuraOrder.api.services.order;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderFilter;
import me.chris.sakuraOrder.api.model.OrderSort;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides access to a player's order history.
 *
 * <p>The history includes the player's live orders
 * ({@code ACTIVE} and {@code FULFILLED}) together with their
 * {@code COMPLETED} orders loaded from persistent storage.</p>
 *
 * <p>Completed history is cached per player after the initial database load
 * to avoid repeatedly querying persistent storage.</p>
 */
public interface OrderHistoryService {

    /**
     * Retrieves a player's order history using the specified filter, sort
     * order, and search query.
     *
     * <p>The first request for a player loads their completed orders from the
     * database. Subsequent requests are served from the in-memory cache until
     * that player's history is invalidated or evicted.</p>
     *
     * @param buyerId the unique identifier of the buyer
     * @param filter the order filter to apply
     * @param sort the order sort to apply
     * @param search the free-text search query; a blank value matches all orders
     * @return a future completed with the filtered, sorted, and searched history
     */
    @NotNull
    CompletableFuture<List<IOrder>> getPlayerHistory(
            @NotNull UUID buyerId,
            @NotNull OrderFilter filter,
            @NotNull OrderSort sort,
            @NotNull String search
    );

    /**
     * Invalidates the cached history of a player.
     *
     * <p>The next request for the player reloads their completed history from
     * persistent storage.</p>
     *
     * @param buyerId the unique identifier of the buyer whose history should
     *                be invalidated
     */
    void invalidate(@NotNull UUID buyerId);

    /**
     * Starts the periodic eviction of idle player histories.
     *
     * <p>This method is safe to call multiple times.</p>
     */
    void start();

    /**
     * Stops the periodic history eviction task.
     */
    void stop();
}