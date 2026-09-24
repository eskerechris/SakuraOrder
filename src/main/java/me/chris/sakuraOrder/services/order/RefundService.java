package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.economy.result.EconomyResult;
import me.chris.sakuraOrder.api.services.order.OrderRefundService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RefundService implements OrderRefundService {

    private final OrderPlugin plugin;

    public RefundService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> refund(@NotNull UUID orderId, @NotNull UUID buyerId, @NotNull BigDecimal amount) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Runnable task = () -> {
            try {
                if (plugin.getEconomyService().deposit(buyerId, amount) instanceof EconomyResult.Failure) {
                    plugin.getLogger().warning("Refund deposit failed for order " + orderId
                            + ": manual verification needed for " + amount + " to " + buyerId);
                    result.complete(false);
                    return;
                }
                result.complete(true);
            } catch (RuntimeException ex) {
                plugin.getLogger().severe("Refund threw for order " + orderId);
                result.complete(false); // never leave the future hanging
            }
        };

        Player online = Bukkit.getPlayer(buyerId);
        if (online != null) {
            SakuraOrder.getInstance().getSchedulerService().runAtEntity(online, task);
        } else {
            SakuraOrder.getInstance().getSchedulerService().runNextTick(task);
        }
        return result;
    }
}