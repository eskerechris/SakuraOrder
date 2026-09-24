package me.chris.sakuraOrder.api.services.order.result;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.order.OrderDeliveryService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the outcome {@link OrderDeliveryService#deliver} attempt.
 *
 * <p>A delivery attempt may fully deliver the provided items, partially
 * deliver them when the order requires fewer items than provided, or fail
 * without delivering any items.</p>
 */
public sealed interface DeliveryResult
        permits DeliveryResult.Delivered, DeliveryResult.Partial, DeliveryResult.Failed {

    /**
     * Represents a successful delivery where all provided items were delivered
     * to the order and no excess items remained.
     *
     * @param order the order that received the delivered items
     * @param deliveredAmount the amount delivered in this attempt
     * @param message the user-facing success message
     */
    record Delivered(
            @NotNull IOrder order,
            int deliveredAmount,
            @NotNull Component message
    ) implements DeliveryResult {}

    /**
     * Represents a successful delivery where the order required fewer items
     * than were provided.
     *
     * <p>The excess items are returned to the deliverer, either to their
     * inventory or, if there is insufficient space, dropped on the ground.</p>
     *
     * @param order the order that received the delivered items
     * @param deliveredAmount the amount actually delivered to the order
     * @param returnedAmount the amount that could not be delivered and was returned
     * @param message the user-facing message describing the partial delivery
     */
    record Partial(
            @NotNull IOrder order,
            int deliveredAmount,
            int returnedAmount,
            @NotNull Component message
    ) implements DeliveryResult {}

    /**
     * Represents a failed delivery attempt where no items were delivered.
     *
     * @param reason the specific reason why the delivery failed
     * @param message the user-facing failure message
     */
    record Failed(
            @NotNull FailureReason reason,
            @NotNull Component message
    ) implements DeliveryResult {}

    /**
     * Defines the possible reasons why a delivery attempt may fail.
     */
    enum FailureReason {

        /**
         * The requested order could not be found.
         */
        ORDER_NOT_FOUND,

        /**
         * The order has no remaining amount that can be delivered.
         */
        ORDER_FULL,

        /**
         * The provided item is not valid for the requested order.
         */
        INVALID_ITEM,

        /**
         * The order system is currently locked and delivery operations are disabled.
         */
        LOCKED,

        /**
         * The delivery attempt failed for an unspecified reason.
         */
        UNKNOWN
    }
}