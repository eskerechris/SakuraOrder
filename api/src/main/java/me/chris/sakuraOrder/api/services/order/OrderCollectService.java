package me.chris.sakuraOrder.api.services.order;

import me.chris.sakuraOrder.api.services.order.result.CollectResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides operations for collecting items that an order has already received
 * but that have not yet been claimed by its buyer.
 *
 * <p>Collection is independent of the order's delivery progress. Partially
 * delivered orders can therefore be collected from immediately without
 * preventing the order from receiving further deliveries.</p>
 */
public interface OrderCollectService {

    /**
     * Collects up to the requested amount of items that have been delivered
     * to an order but not yet collected.
     *
     * <p>The collector must be the buyer of the order. The amount collected is
     * determined atomically and may be lower than the requested amount if fewer
     * items are available at the time of the reservation.</p>
     *
     * @param orderId the unique identifier of the order to collect from
     * @param collector the player collecting the items; must be the order's buyer
     * @param requestedAmount the maximum amount to collect; must be positive
     * @param mode the method used to deliver the collected items to the collector
     * @return a future completed with the result of the collection attempt
     */
    @NotNull
    CompletableFuture<CollectResult> collect(
            @NotNull UUID orderId,
            @NotNull Player collector,
            int requestedAmount,
            @NotNull CollectMode mode
    );

    /**
     * Defines how collected items are delivered to the buyer.
     */
    enum CollectMode {

        /**
         * Adds collected items directly to the buyer's inventory.
         *
         * <p>Any items that cannot fit in the inventory are dropped at the
         * buyer's location.</p>
         */
        TO_INVENTORY,

        /**
         * Drops all collected items at the buyer's location without attempting
         * to add them to the inventory.
         */
        DROP_AT_FEET
    }
}