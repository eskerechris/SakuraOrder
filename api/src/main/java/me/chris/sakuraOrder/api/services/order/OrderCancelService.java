package me.chris.sakuraOrder.api.services.order;

import me.chris.sakuraOrder.api.services.order.result.FinalizeResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides operations for closing active orders before they have been fully delivered.
 */
public interface OrderCancelService {

    /**
     * Closes an active order early on behalf of its buyer.
     *
     * <p>The order amount is reduced to the amount already delivered and the order
     * transitions to {@code FULFILLED}, starting its claim grace period. If all
     * delivered items have already been collected, the order transitions directly
     * to {@code COMPLETED}. This also applies when the order has an amount of zero.</p>
     *
     * <p>The undelivered amount is refunded to the buyer. The operation fails if
     * the order does not exist, does not belong to the specified buyer, is no
     * longer active, or the refund cannot be processed.</p>
     *
     * @param orderId the unique identifier of the order to finalize
     * @param buyer the player requesting the finalization; must be the order's buyer
     * @return a future completed with the result of the finalization attempt
     */
    @NotNull
    CompletableFuture<FinalizeResult> finalizeEarly(
            @NotNull UUID orderId,
            @NotNull Player buyer
    );

    /**
     * Closes an active order early on behalf of an administrator.
     *
     * <p>This operation can finalize an order regardless of its buyer. The
     * undelivered amount is refunded to the order's buyer, even if the buyer
     * is currently offline.</p>
     *
     * <p>Permission checks are the responsibility of the caller.</p>
     *
     * @param orderId the unique identifier of the order to finalize
     * @return a future completed with the result of the finalization attempt
     */
    @NotNull
    CompletableFuture<FinalizeResult> finalizeEarlyAsAdmin(@NotNull UUID orderId);
}