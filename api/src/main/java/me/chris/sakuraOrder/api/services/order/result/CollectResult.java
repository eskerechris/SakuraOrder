package me.chris.sakuraOrder.api.services.order.result;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderStatus;
import me.chris.sakuraOrder.api.services.order.OrderCollectService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the result of an {@link OrderCollectService#collect} attempt.
 *
 * <p>A collection attempt either succeeds and returns the updated order and
 * collected amount, or fails with a specific reason and localized feedback.</p>
 */
public sealed interface CollectResult {

    /**
     * Represents a successful collection.
     *
     * <p>The collected amount may be lower than the amount originally requested
     * if fewer items were available at the time of the collection.</p>
     *
     * @param order the order after the collection, reflecting the updated collected
     *              amount and potentially a transition to {@link OrderStatus#COMPLETED}
     * @param collectedAmount the amount of items actually collected
     * @param message localized feedback for the collector
     */
    record Collected(
            @NotNull IOrder order,
            int collectedAmount,
            @NotNull Component message
    ) implements CollectResult {}

    /**
     * Represents a failed collection attempt.
     *
     * @param reason the reason why the collection failed
     * @param message localized feedback for the collector
     */
    record Failed(
            @NotNull FailureReason reason,
            @NotNull Component message
    ) implements CollectResult {}

    /**
     * Defines the possible reasons why an order collection may fail.
     */
    enum FailureReason {

        /**
         * The requested order could not be found.
         */
        ORDER_NOT_FOUND,

        /**
         * The player attempting to collect the order is not its buyer.
         */
        NOT_BUYER,

        /**
         * The order has no items available for collection.
         */
        NOTHING_TO_COLLECT,

        /**
         * The collection request contains invalid parameters.
         */
        INVALID_REQUEST,

        /**
         * The order system is currently locked and collection operations are disabled.
         */
        LOCKED
    }
}