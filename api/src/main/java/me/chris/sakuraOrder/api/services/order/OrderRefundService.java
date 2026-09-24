package me.chris.sakuraOrder.api.services.order;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Deposits refunds to order buyers on the correct thread.
 *
 */
public interface OrderRefundService {

    /**
     * @return a future completed with {@code true} if the deposit succeeded,
     *         {@code false} otherwise (never completes exceptionally)
     */
    @NotNull
    CompletableFuture<Boolean> refund(@NotNull UUID orderId, @NotNull UUID buyerId, @NotNull BigDecimal amount);
}