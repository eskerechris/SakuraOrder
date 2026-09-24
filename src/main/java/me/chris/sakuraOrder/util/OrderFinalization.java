package me.chris.sakuraOrder.util;

import me.chris.sakuraOrder.api.model.FinalizeReservation;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderStatus;
import me.chris.sakuraOrder.api.model.OrderTransition;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Pure finalization rules, meant to run inside {@code OrderCacheService#transition}.
 */
public final class OrderFinalization {

    private OrderFinalization() {}

    /**
     * Early finalization: caps {@code amount} to {@code delivered}, moves the order to
     * {@code FULFILLED} with a new expiry, or straight to {@code COMPLETED} if everything
     * was already collected. Refundable amount is {@code amount - delivered}.
     */
    @NotNull
    public static OrderTransition.Outcome<FinalizeReservation> decide(
            @NotNull IOrder current,
            @NotNull Instant newExpiresAt
    ) {
        if (current.getOrderStatus() != OrderStatus.ACTIVE) {
            return OrderTransition.Outcome.unchanged(
                    current,
                    new FinalizeReservation.Rejected(
                            FinalizeReservation.FailureReason.ALREADY_FULFILLED
                    )
            );
        }

        long refundable = Math.max(0L, current.getAmount() - current.getDelivered());

        IOrder fulfilled = OrderCopies.withAmountAndStatus(
                current,
                current.getDelivered(),
                OrderStatus.FULFILLED,
                newExpiresAt
        );

        IOrder next = fulfilled.getCollected() >= fulfilled.getAmount()
                ? OrderCopies.withStatus(fulfilled, OrderStatus.COMPLETED)
                : fulfilled;

        return OrderTransition.Outcome.changed(
                next,
                new FinalizeReservation.Applied(next, refundable)
        );
    }

    /**
     * Expiry rule. {@code ACTIVE} with nothing delivered becomes {@code EXPIRED} with a full
     * refund; {@code ACTIVE} with deliveries follows {@link #decide}; {@code FULFILLED}
     * becomes {@code COMPLETED} (uncollected items are forfeited, no refund).
     * Orders that are not expired, or in any other status, are left unchanged.
     */
    @NotNull
    public static OrderTransition.Outcome<FinalizeReservation> decideExpiry(
            @NotNull IOrder current,
            @NotNull Instant graceExpiresAt
    ) {
        if (!current.isExpired()) {
            return notApplicable(current);
        }

        return switch (current.getOrderStatus()) {
            case ACTIVE -> {
                if (current.getDelivered() > 0) {
                    yield decide(current, graceExpiresAt);
                }
                IOrder expired = OrderCopies.withStatus(current, OrderStatus.EXPIRED);
                yield OrderTransition.Outcome.changed(
                        expired,
                        new FinalizeReservation.Applied(expired, current.getAmount())
                );
            }
            case FULFILLED -> {
                IOrder completed = OrderCopies.withStatus(current, OrderStatus.COMPLETED);
                yield OrderTransition.Outcome.changed(
                        completed,
                        new FinalizeReservation.Applied(completed, 0L)
                );
            }
            default -> notApplicable(current);
        };
    }

    @NotNull
    private static OrderTransition.Outcome<FinalizeReservation> notApplicable(@NotNull IOrder current) {
        return OrderTransition.Outcome.unchanged(
                current,
                new FinalizeReservation.Rejected(FinalizeReservation.FailureReason.ALREADY_FULFILLED)
        );
    }
}