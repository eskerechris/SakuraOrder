package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.order.result.FinalizeResult;
import me.chris.sakuraOrder.menu.DeliveryDepositMenu;
import me.chris.sakuraOrder.menu.EditOrderMenu;
import me.chris.sakuraOrder.menu.OrderMenu;
import me.chris.sakuraOrder.menu.YourOrdersMenu;
import me.chris.sakuraOrder.menu.framework.DynamicButton;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.StringUtil;
import me.chris.sakuraOrder.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Displays a single active order with a dynamically updating time-remaining lore line.
 */
public class OrderButton extends DynamicButton {

    private final OrderPlugin plugin;
    private final UUID orderId;
    private final Menu parent;

    private static final String REMOVE_PERMISSION = "sakuraorder.command.remove";

    public OrderButton(int slot, @NotNull UUID orderId, Menu parent, @NotNull OrderPlugin plugin) {
        super(slot);
        this.orderId = orderId;
        this.plugin = plugin;
        this.parent = parent;
    }

    @Override
    @NotNull
    protected ItemStack createItem() {
        IOrder order = plugin.getOrderCacheService().get(orderId);
        ItemStack item = (order != null) ? order.getItemStack() : new ItemStack(Material.BARRIER);
        applyMeta(item, order);
        return item;
    }

    @Override
    public void update() {
        IOrder order = plugin.getOrderCacheService().get(orderId);
        applyMeta(getItem(), order);
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        IOrder order = plugin.getOrderCacheService().get(orderId);
        if (order == null) return;

        if (clickType.isShiftClick() && parent instanceof OrderMenu) {
            if (player.hasPermission(REMOVE_PERMISSION)) {
                plugin.getOrderCancelService().finalizeEarlyAsAdmin(orderId).thenAccept(result -> {
                    if (result instanceof FinalizeResult.Success success) {
                        player.sendMessage(success.message());
                    } else if (result instanceof FinalizeResult.Failed failed) {
                        player.sendMessage(failed.message());
                    }
                });
            }
            return;
        }

        if (parent instanceof YourOrdersMenu && order.getBuyerId().equals(player.getUniqueId())) {
           new EditOrderMenu(plugin, orderId).displayTo(player);
           return;
        }

        if (parent instanceof OrderMenu && !order.getBuyerId().equals(player.getUniqueId())) {
            new DeliveryDepositMenu(plugin, orderId).displayTo(player);
        }
    }

    private void applyMeta(@NotNull ItemStack item, @Nullable IOrder order) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (order == null) {
            meta.displayName(plugin.getLangService().menuMessage("order-menu.order-not-available"));
            meta.lore(List.of());
            item.setItemMeta(meta);
            return;
        }

        String buyer_name = Bukkit.getOfflinePlayer(order.getBuyerId()).getName();

        meta.displayName(plugin.getLangService().menuMessage("order-menu.order-button.name",
                Map.of("buyer_name", buyer_name != null ? buyer_name : "Unknown")));

        String remaining = (order.getExpiresAt() != null)
                ? TimeFormatter.formatRemaining(Duration.between(Instant.now(), order.getExpiresAt()))
                : "∞";

        Map<String, String> placeholders = Map.of(
                "item_amount", NumberParser.formatNumber(order.getOriginalAmount()),
                "item_name", StringUtil.formatMaterial(order.getItemStack()),
                "price_each", NumberParser.formatNumber(order.getPricePerItem()),
                "delivered", NumberParser.formatNumber(order.getDelivered()),
                "paid", NumberParser.formatNumber(order.getAmountPaid()),
                "total_price", NumberParser.formatNumber(order.getTotalPrice()),
                "time_left", remaining
        );

        List<Component> lore = new ArrayList<>(
                plugin.getLangService().menuMessageList("order-menu.order-button.lore", placeholders)
        );

        Player viewer = parent.getViewer();
        if (parent instanceof OrderMenu && viewer != null && viewer.hasPermission(REMOVE_PERMISSION)) {
            lore.add(Component.empty());
            lore.add(plugin.getLangService().menuMessage("order-menu.order-button.lore-admin"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
    }
}