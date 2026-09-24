package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.FinalizeReservation;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderTransition;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.api.services.order.OrderCacheService;
import me.chris.sakuraOrder.api.services.order.OrderCancelService;
import me.chris.sakuraOrder.api.services.order.OrderRefundService;
import me.chris.sakuraOrder.api.services.order.result.FinalizeResult;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.OrderFinalization;
import me.chris.sakuraOrder.util.OrderMaintenanceLock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles early finalization of orders and refunds.
 */
public class CancelService implements OrderCancelService {

    private final OrderPlugin plugin;
    private final LangService lang;
    private final OrderCacheService cacheService;
    private final OrderRefundService refundService;

    public CancelService(
            @NotNull OrderPlugin plugin,
            @NotNull OrderCacheService cacheService,
            @NotNull OrderRefundService refundService
    ) {
        this.plugin = plugin;
        this.lang = plugin.getLangService();
        this.cacheService = cacheService;
        this.refundService = refundService;
    }

    @Override
    @NotNull
    public CompletableFuture<FinalizeResult> finalizeEarly(@NotNull UUID orderId, @NotNull Player buyer) {
        if (OrderMaintenanceLock.isLocked()) {
            return CompletableFuture.completedFuture(
                    new FinalizeResult.Failed(
                            FinalizeResult.FailureReason.LOCKED,
                            lang.message("order.locked")
                    )
            );
        }
        return finalizeInternal(orderId, buyer.getUniqueId());
    }

    @Override
    @NotNull
    public CompletableFuture<FinalizeResult> finalizeEarlyAsAdmin(@NotNull UUID orderId) {
        return finalizeInternal(orderId, null);
    }

    /**
     * Performs the shared finalization flow for buyer and admin requests.
     *
     * @param orderId the unique identifier of the order
     * @param requiredBuyerId the buyer required to own the order, or {@code null}
     *                       to skip the ownership check
     * @return a future completed with the finalization result
     */
    @NotNull
    private CompletableFuture<FinalizeResult> finalizeInternal(
            @NotNull UUID orderId,
            @Nullable UUID requiredBuyerId
    ) {
        IOrder snapshot = cacheService.get(orderId);
        if (snapshot == null) {
            return CompletableFuture.completedFuture(
                    new FinalizeResult.Failed(
                            FinalizeResult.FailureReason.ORDER_NOT_FOUND,
                            lang.message("order.not-found")
                    )
            );
        }

        if (requiredBuyerId != null && !snapshot.getBuyerId().equals(requiredBuyerId)) {
            return CompletableFuture.completedFuture(
                    new FinalizeResult.Failed(
                            FinalizeResult.FailureReason.NOT_BUYER,
                            lang.message("order.not-your-order")
                    )
            );
        }

        FinalizeReservation reservation = reserveFinalize(orderId);

        if (reservation instanceof FinalizeReservation.Rejected(
                FinalizeReservation.FailureReason rejectionReason)) {

            return switch (rejectionReason) {
                case ORDER_NOT_FOUND -> CompletableFuture.completedFuture(
                        new FinalizeResult.Failed(
                                FinalizeResult.FailureReason.ORDER_NOT_FOUND,
                                lang.message("order.not-found")
                        )
                );
                case ALREADY_FULFILLED -> CompletableFuture.completedFuture(
                        new FinalizeResult.Failed(
                                FinalizeResult.FailureReason.ALREADY_FULFILLED,
                                lang.message("order.not-enough-space")
                        )
                );
            };
        }

        FinalizeReservation.Applied applied = (FinalizeReservation.Applied) reservation;
        IOrder updatedOrder = applied.order();
        long refundableAmount = applied.refundableAmount();

        if (refundableAmount <= 0) {
            return CompletableFuture.completedFuture(
                    new FinalizeResult.Success(
                            updatedOrder,
                            BigDecimal.ZERO,
                            lang.message("order.deleted")
                    )
            );
        }

        BigDecimal refund = updatedOrder.getPricePerItem().multiply(BigDecimal.valueOf(refundableAmount));

        return refundService.refund(orderId, updatedOrder.getBuyerId(), refund)
                .thenApply(ok -> ok
                        ? new FinalizeResult.Success(
                        updatedOrder,
                        refund,
                        lang.message("order.deleted",
                                Map.of("refund", NumberParser.formatNumber(refund))))
                        : new FinalizeResult.Failed(
                        FinalizeResult.FailureReason.ECONOMY_ERROR,
                        lang.message("order.refund-failed")));
    }

    @NotNull
    private FinalizeReservation reserveFinalize(@NotNull UUID orderId) {
        int graceDays = plugin.getSettingsService().getGeneral().getClaimGraceDays();
        Instant newExpiresAt = Instant.now().plus(graceDays, ChronoUnit.DAYS);

        OrderTransition.Outcome<FinalizeReservation> outcome =
                cacheService.transition(orderId, current -> OrderFinalization.decide(current, newExpiresAt));

        if (outcome == null) {
            return new FinalizeReservation.Rejected(FinalizeReservation.FailureReason.ORDER_NOT_FOUND);
        }

        return Objects.requireNonNull(outcome.value());
    }
}