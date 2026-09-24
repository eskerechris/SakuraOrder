package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.FinalizeReservation;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderTransition;
import me.chris.sakuraOrder.api.services.order.OrderCacheService;
import me.chris.sakuraOrder.api.services.order.OrderExpirationService;
import me.chris.sakuraOrder.api.services.order.OrderRefundService;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.OrderFinalization;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Expires overdue orders and refunds the undelivered portion to the buyer.
 */
public class ExpirationService implements OrderExpirationService {

    private final OrderPlugin plugin;
    private final OrderCacheService cacheService;
    private final OrderRefundService refundService;

    private WrappedTask wrappedTask;

    public ExpirationService(
            @NotNull OrderPlugin plugin,
            @NotNull OrderCacheService cacheService,
            @NotNull OrderRefundService refundService
    ) {
        this.plugin = plugin;
        this.cacheService = cacheService;
        this.refundService = refundService;
    }

    @Override
    public void start() {
        stop(); // avoid duplicate tasks if called multiple times
        this.wrappedTask = SakuraOrder.getInstance().getSchedulerService().runTimerAsync(
                this::checkExpirations,
                20L * 60,
                20L * 30
        );
    }

    @Override
    public void stop() {
        if (wrappedTask != null && !wrappedTask.isCancelled()) {
            wrappedTask.cancel();
        }
    }

    private void checkExpirations() {
        try {
            int graceDays = plugin.getSettingsService().getGeneral().getClaimGraceDays();

            // The snapshot only yields candidates: each one is revalidated and transitioned
            // atomically inside transition(), which also enqueues persistence.
            for (IOrder candidate : cacheService.getExpired()) {
                try {
                    expire(candidate.getId(), graceDays);
                } catch (RuntimeException ex) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Failed to expire order " + candidate.getId(), ex);
                }
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Expiration check failed", ex);
        }
    }

    private void expire(@NotNull UUID id, int graceDays) {
        Instant graceExpiresAt = Instant.now().plus(graceDays, ChronoUnit.DAYS);

        OrderTransition.Outcome<FinalizeReservation> outcome =
                cacheService.transition(id, current -> OrderFinalization.decideExpiry(current, graceExpiresAt));

        if (outcome == null) {
            return; // no longer in the live cache
        }
        if (!(outcome.value() instanceof FinalizeReservation.Applied(IOrder order, long refundableAmount))) {
            return; // another thread changed the order first
        }
        if (refundableAmount <= 0) {
            return; // nothing to refund (e.g. FULFILLED -> COMPLETED)
        }

        // The refund is computed inside the atomic transition, so it can never be issued
        // twice even when expiry and cancel race on the same order.
        BigDecimal refund = order.getPricePerItem()
                .multiply(BigDecimal.valueOf(refundableAmount));

        refundService.refund(id, order.getBuyerId(), refund).thenAccept(ok -> {
            if (!ok) {
                return; // already logged by the refund service
            }
            Player buyer = Bukkit.getPlayer(order.getBuyerId());
            if (buyer != null) {
                buyer.sendMessage(plugin.getLangService().message(
                        "order.expired-refund",
                        Map.of("refund", NumberParser.formatNumber(refund))
                ));
            }
        });
    }
}