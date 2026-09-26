package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.event.create.OrderCreateEvent;
import me.chris.sakuraOrder.api.event.create.OrderPreCreateEvent;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.api.services.economy.EconomyService;
import me.chris.sakuraOrder.api.services.economy.result.EconomyResult;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.api.services.order.OrderCreateService;
import me.chris.sakuraOrder.api.services.order.result.CreateResult;
import me.chris.sakuraOrder.api.services.order.result.CreateResult.FailureReason;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.OrderLimitResolver;
import me.chris.sakuraOrder.util.OrderMaintenanceLock;
import me.chris.sakuraOrder.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CreateService implements OrderCreateService {

    private final OrderPlugin plugin;
    private final LangService lang;

    public CreateService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLangService();
    }

    @Override
    @NotNull
    public CompletableFuture<CreateResult> createOrder(@NotNull IOrder order) {

        if (OrderMaintenanceLock.isLocked()) {
            return CompletableFuture.completedFuture(
             new CreateResult.Failed(FailureReason.LOCKED, lang.message("order.locked"))
            );
        }

        // Instant domain validation
        int configuredDefault = plugin.getSettingsService().getGeneral().getMaxOrderSupported();
        Player buyer = Bukkit.getPlayer(order.getBuyerId());
        int limit = buyer != null
                ? OrderLimitResolver.resolve(buyer, configuredDefault)
                : configuredDefault;

        int activeOrders = plugin.getOrderCacheService().getActiveByBuyer(order.getBuyerId()).size();

        if (activeOrders >= limit) {
            return CompletableFuture.completedFuture(
                    new CreateResult.Failed(
                            FailureReason.MAX_AMOUNT_REACHED,
                            lang.message("order.max-amount-reached", Map.of("limit", String.valueOf(limit)))
                    )
            );
        }

        if (order.getAmount() <= 0) {
            return CompletableFuture.completedFuture(
                    new CreateResult.Failed(FailureReason.VALIDATION_ERROR, lang.message("order.invalid-item-amount"))
            );
        }

        if (order.getAmount() > NumberParser.parseLong(plugin.getSettingsService().getGeneral().getOrderMaxItemAmount())) {
            return CompletableFuture.completedFuture(
                    new CreateResult.Failed(FailureReason.VALIDATION_ERROR, lang.message("order.invalid-item-amount"))
            );
        }

        if (order.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(
                    new CreateResult.Failed(FailureReason.VALIDATION_ERROR, lang.message("order.invalid-price-amount"))
            );
        }

        // Economy pre-check (fail-fast)
        EconomyService economy = plugin.getEconomyService();
        BigDecimal totalPrice = order.getTotalPrice();

        if (!economy.isEnabled()) {
            return CompletableFuture.completedFuture(
                    new CreateResult.Failed(FailureReason.ECONOMY_UNAVAILABLE, lang.message("order.economy-unavailable"))
            );
        }

        if (!economy.has(order.getBuyerId(), totalPrice)) {
            return CompletableFuture.completedFuture(
                    new CreateResult.Failed(FailureReason.INSUFFICIENT_FUNDS, lang.message("order.insufficient-funds"))
            );
        }

        // Synchronous Pre-Create event dispatch on the main server thread
        CompletableFuture<CreateResult> preEventFuture = new CompletableFuture<>();

        SakuraOrder.getInstance().getSchedulerService().runNextTick(() -> {
            var preCreateEvent = new OrderPreCreateEvent(order);
            if (!preCreateEvent.callEvent()) {
                Component cancelReason = Component.text(preCreateEvent.getReason());
                preEventFuture.complete(new CreateResult.Failed(FailureReason.CANCELLED_BY_LISTENER, cancelReason));
            } else {
                preEventFuture.complete(null); // Signal approval to proceed
            }
        });

        // Asynchronous persistence & cache update (only if PreEvent approved)
        return preEventFuture.thenCompose(preResult -> {
            if (preResult != null) {
                return CompletableFuture.completedFuture(preResult);
            }

            // The actual charge is only made now that the order has been approved by the listeners
            EconomyResult withdrawResult = economy.withdraw(order.getBuyerId(), totalPrice);
            if (withdrawResult instanceof EconomyResult.Failure) {
                return CompletableFuture.completedFuture(
                        new CreateResult.Failed(FailureReason.INSUFFICIENT_FUNDS, lang.message("order.insufficient-funds"))
                );
            }

            return plugin.getOrderRepository().save(order)
                    .thenApply(v -> {
                        plugin.getOrderCacheService().put(order);
                        return (CreateResult) new CreateResult.Created(order, lang.message("order.created"));
                    })
                    .exceptionally(e -> {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        // Offsetting transaction: refund because the order was not saved
                        economy.deposit(order.getBuyerId(), totalPrice);
                        return new CreateResult.Failed(
                                FailureReason.DATABASE_ERROR,
                                lang.message("order.creation-failed")
                        );
                    });
        }).thenCompose(result -> {
            // Synchronous Post-Create event dispatch (only on success)
            if (result instanceof CreateResult.Created createdResult) {
                CompletableFuture<CreateResult> postEventFuture = new CompletableFuture<>();

                SakuraOrder.getInstance().getSchedulerService().runNextTick(() -> {
                    new OrderCreateEvent(createdResult.order()).callEvent();

                    if (plugin.getSettingsService().getGeneral().shouldBroadcast()) {
                        String buyer_name = Bukkit.getOfflinePlayer(order.getBuyerId()).getName();
                        Bukkit.broadcast(lang.message("order.created-broadcast",
                                Map.of(
                                        "buyer_name", buyer_name != null ? buyer_name : "Unknown",
                                        "amount", NumberParser.formatNumber(order.getAmount()),
                                        "item_name", StringUtil.formatMaterial(order.getItemStack()),
                                        "price_each", NumberParser.formatNumber(order.getPricePerItem())
                                )));
                    }

                    Player player = Bukkit.getPlayer(order.getBuyerId());
                    if (player != null) {
                        plugin.getSoundService().play(player, SakuraSound.ORDER_CREATE);
                    }

                    postEventFuture.complete(createdResult);
                });

                return postEventFuture;
            }

            return CompletableFuture.completedFuture(result);
        });
    }
}