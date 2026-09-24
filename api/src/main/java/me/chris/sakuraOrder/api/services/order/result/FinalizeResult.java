package me.chris.sakuraOrder.api.services.order.result;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.order.OrderCancelService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * Represents the outcome of an {@link OrderCancelService#finalizeEarly} attempt.
 *
 * <p>An early finalization either succeeds and closes the order, potentially
 * refunding the buyer for any remaining undelivered amount, or fails with a
 * specific reason.</p>
 */
public sealed interface FinalizeResult {

    /**
     * Represents a successful early finalization.
     *
     * <p>If the order still has an undelivered amount, that amount is refunded
     * to the buyer. No refund is issued when the order has already been fully
     * delivered.</p>
     *
     * @param order the order after finalization
     * @param refunded the amount refunded to the buyer
     * @param message the user-facing finalization message
     */
    record Success(
            @NotNull IOrder order,
            @NotNull BigDecimal refunded,
            @NotNull Component message
    ) implements FinalizeResult {}

    /**
     * Represents a failed early finalization attempt.
     *
     * @param reason the specific reason why finalization failed
     * @param message the user-facing failure message
     */
    record Failed(
            @NotNull FailureReason reason,
            @NotNull Component message
    ) implements FinalizeResult {}

    /**
     * Defines the possible reasons why an early finalization attempt may fail.
     */
    enum FailureReason {

        /**
         * The requested order could not be found.
         */
        ORDER_NOT_FOUND,

        /**
         * The player attempting to finalize the order is not its buyer.
         */
        NOT_BUYER,

        /**
         * The order has already been fulfilled and cannot be finalized early.
         */
        ALREADY_FULFILLED,

        /**
         * An error occurred while processing the refund through the economy provider.
         */
        ECONOMY_ERROR,

        /**
         * The order system is currently locked and collection operations are disabled.
         */
        LOCKED
    }
}