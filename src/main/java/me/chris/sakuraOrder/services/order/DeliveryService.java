package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.DeliveryAllocation;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.economy.result.EconomyResult;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.api.services.order.OrderDeliveryService;
import me.chris.sakuraOrder.api.services.order.result.DeliveryResult;
import me.chris.sakuraOrder.api.services.order.result.DeliveryResult.FailureReason;
import me.chris.sakuraOrder.util.DeliveryCalculator;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.OrderMaintenanceLock;
import me.chris.sakuraOrder.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles order item deliveries and player-facing delivery effects.
 */
public class DeliveryService implements OrderDeliveryService {

    private final OrderPlugin plugin;
    private final LangService lang;

    public DeliveryService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLangService();
    }

    @Override
    @NotNull
    public CompletableFuture<DeliveryResult> deliver(
            @NotNull UUID orderId,
            @NotNull List<ItemStack> providedStacks,
            @NotNull Player deliverer
    ) {
        if (OrderMaintenanceLock.isLocked()) {
            return CompletableFuture.completedFuture(
                    new DeliveryResult.Failed(FailureReason.LOCKED, lang.message("order.locked"))
            );
        }

        IOrder snapshot = plugin.getOrderCacheService().get(orderId);
        if (snapshot == null) {
            returnAndFinish(providedStacks, BigDecimal.ZERO, deliverer);
            return CompletableFuture.completedFuture(
                    new DeliveryResult.Failed(FailureReason.ORDER_NOT_FOUND, lang.message("order.not-found"))
            );
        }

        // Snapshot used only for item matching, the remaining amount is
        // re-validated atomically by reserveDelivery().
        DeliveryCalculator.ValiditySplit split =
                DeliveryCalculator.splitByValidity(snapshot, providedStacks);

        int validAmount = split.validAmount();
        int invalidAmount = split.invalidAmount();

        if (validAmount <= 0) {
            returnAndFinish(providedStacks, BigDecimal.ZERO, deliverer);
            return CompletableFuture.completedFuture(
                    new DeliveryResult.Failed(
                            FailureReason.INVALID_ITEM,
                            lang.message("order.invalid-delivery-item")
                    )
            );
        }

        DeliveryAllocation allocation =
                plugin.getOrderCacheService().reserveDelivery(orderId, validAmount);

        if (allocation == null) {
            returnAndFinish(providedStacks, BigDecimal.ZERO, deliverer);
            return CompletableFuture.completedFuture(
                    new DeliveryResult.Failed(
                            FailureReason.ORDER_NOT_FOUND,
                            lang.message("order.not-found")
                    )
            );
        }

        int deliveredNow = allocation.deliveredAmount();
        if (deliveredNow == 0) {
            returnAndFinish(providedStacks, BigDecimal.ZERO, deliverer);
            return CompletableFuture.completedFuture(
                    new DeliveryResult.Failed(
                            FailureReason.ORDER_FULL,
                            lang.message("order.not-enough-space")
                    )
            );
        }

        IOrder updatedOrder = allocation.order();
        int excessValid = validAmount - deliveredNow;

        List<ItemStack> toReturn = new ArrayList<>(split.invalidStacks());
        if (excessValid > 0) {
            toReturn.addAll(
                    splitIntoStacks(updatedOrder.getItemStack().getType(), excessValid)
            );
        }

        BigDecimal earned = updatedOrder.getPricePerItem()
                .multiply(BigDecimal.valueOf(deliveredNow));

        returnAndFinish(toReturn, earned, deliverer);

        if (updatedOrder.getDelivered() >= updatedOrder.getAmount()) {
            plugin.getOrderCacheService().fulfill(updatedOrder);
        }

        String buyerName = Bukkit.getOfflinePlayer(updatedOrder.getBuyerId()).getName();

        Map<String, String> placeholders = Map.of(
                "amount", NumberParser.formatNumber(deliveredNow),
                "item_name", StringUtil.formatMaterial(updatedOrder.getItemStack()),
                "buyer_name", buyerName != null ? buyerName : "Unknown"
        );

        notifyBuyer(updatedOrder, deliveredNow, deliverer);

        DeliveryResult result =
                (excessValid > 0 || !split.invalidStacks().isEmpty())
                        ? new DeliveryResult.Partial(
                        updatedOrder,
                        deliveredNow,
                        excessValid + invalidAmount,
                        lang.message("order.delivered", placeholders)
                )
                        : new DeliveryResult.Delivered(
                        updatedOrder,
                        deliveredNow,
                        lang.message("order.delivered", placeholders)
                );

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Applies the physical delivery effects on the deliverer's owning thread.
     *
     * <p>Undeliverable items are added to the inventory first, with any overflow
     * dropped at the player's location. The delivered amount is credited through
     * the economy service.</p>
     */
    private void returnAndFinish(
            @NotNull List<ItemStack> toReturn,
            @NotNull BigDecimal earned,
            @NotNull Player player
    ) {
        SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> {
            if (!toReturn.isEmpty()) {
                var leftover = player.getInventory().addItem(
                        toReturn.toArray(new ItemStack[0])
                );
                leftover.values().forEach(stack ->
                        player.getWorld().dropItemNaturally(player.getLocation(), stack)
                );
            }

            if (earned.compareTo(BigDecimal.ZERO) > 0) {
                EconomyResult payResult =
                        plugin.getEconomyService().deposit(player.getUniqueId(), earned);

                if (payResult instanceof EconomyResult.Failure) {
                    plugin.getLogger().warning(
                            "Deposit failed for delivery: items already deducted/returned, "
                                    + "payment of " + earned
                                    + " requires manual verification for "
                                    + player.getUniqueId()
                    );
                }
            }
        });
    }

    private List<ItemStack> splitIntoStacks(@NotNull Material material, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        int maxStack = material.getMaxStackSize();

        while (remaining > 0) {
            int chunk = Math.min(remaining, maxStack);
            stacks.add(new ItemStack(material, chunk));
            remaining -= chunk;
        }

        return stacks;
    }

    private void notifyBuyer(@NotNull IOrder order, int deliveredNow, @NotNull Player deliverer) {
        Player buyer = Bukkit.getPlayer(order.getBuyerId());
        if (buyer == null || !buyer.isOnline()) {
            return;
        }

        Map<String, String> placeholders = Map.of(
                "amount", NumberParser.formatNumber(deliveredNow),
                "item_name", StringUtil.formatMaterial(order.getItemStack()),
                "deliverer_name", deliverer.getName(),
                "delivered", NumberParser.formatNumber(order.getDelivered()),
                "total", NumberParser.formatNumber(order.getAmount())
        );

        buyer.sendMessage(lang.message("order.received-delivery", placeholders));
    }
}