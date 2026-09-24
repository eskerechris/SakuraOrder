package me.chris.sakuraOrder.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * Result of an atomic early-finalize reservation
 */
public sealed interface FinalizeReservation {

    /**
     * The order was moved to {@link OrderStatus#FULFILLED} with {@code amount} reduced to
     * whatever was already {@code delivered}: it will not accept further deliveries and
     * becomes immediately collectable for what has arrived so far.
     *
     * @param order            the order after the reservation
     * @param refundableAmount the quantity that will never be delivered now (the original
     *                         residual), used by the caller to compute the money refund
     */
    record Applied(@NotNull IOrder order, long refundableAmount) implements FinalizeReservation {}

    record Rejected(@NotNull FailureReason reason) implements FinalizeReservation {}

    enum FailureReason {
        ORDER_NOT_FOUND,
        ALREADY_FULFILLED
    }
}