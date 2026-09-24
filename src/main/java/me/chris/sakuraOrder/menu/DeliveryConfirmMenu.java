package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.order.result.DeliveryResult;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Confirmation menu for reviewing and confirming a pending delivery.
 */
public class DeliveryConfirmMenu extends Menu {

    private static final int CANCEL_SLOT = 10;
    private static final int INFO_SLOT = 13;
    private static final int CONFIRM_SLOT = 16;

    private final UUID orderId;
    private final List<ItemStack> depositedItems;

    /**
     * Prevents {@link #onClose(Player)} from returning items after the delivery
     * has already been resolved or handed off to the delivery service.
     */
    private boolean resolved = false;

    public DeliveryConfirmMenu(@NotNull OrderPlugin plugin, @NotNull UUID orderId, @NotNull List<ItemStack> depositedItems) {
        super(plugin);
        this.orderId = orderId;
        this.depositedItems = depositedItems;
        setSize(9 * 3);
        setTitle(plugin.getLangService().menuMessage("delivery-confirm-menu.title"));
    }

    @Override
    protected void registerButtons() {
        IOrder order = getPlugin().getOrderCacheService().get(orderId);

        addButton(Button.of(CANCEL_SLOT, buildCancelItem(), (player, clickType) -> cancel(player)));
        addButton(Button.of(INFO_SLOT, buildInfoItem(order), (player, clickType) -> {}));
        addButton(Button.of(CONFIRM_SLOT, buildConfirmItem(order), (player, clickType) -> confirm(player, order)));
    }

    @Override
    public void onClose(@NotNull Player player) {
        super.onClose(player);
        if (!resolved) {
            returnDepositedItems(player);
        }
    }

    private void cancel(@NotNull Player player) {
        resolved = true;
        returnDepositedItems(player);
        player.closeInventory();
    }

    private void confirm(@NotNull Player player, @Nullable IOrder order) {
        if (order == null) {
            // The order disappeared in the meantime, treat it as a cancellation.
            resolved = true;
            returnDepositedItems(player);
            player.closeInventory();
            return;
        }

        resolved = true;
        getPlugin().getOrderDeliveryService().deliver(orderId, depositedItems, player)
                .thenAccept(result -> player.sendMessage(messageFor(result)));

        // go back to the delivery menu
        new DeliveryDepositMenu(getPlugin(), orderId).displayTo(player);
    }

    @NotNull
    private Component messageFor(@NotNull DeliveryResult result) {
        return switch (result) {
            case DeliveryResult.Delivered d -> d.message();
            case DeliveryResult.Partial p -> p.message();
            case DeliveryResult.Failed f -> f.message();
        };
    }

    private void returnDepositedItems(@NotNull Player player) {
        if (depositedItems.isEmpty()) return;
        var leftover = player.getInventory().addItem(depositedItems.toArray(new ItemStack[0]));
        leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    private ItemStack buildCancelItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(getPlugin().getLangService().menuMessage("delivery-confirm-menu.cancel-button.name"));
        meta.lore(getPlugin().getLangService().menuMessageList("delivery-confirm-menu.cancel-button.lore"));

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack buildInfoItem(@Nullable IOrder order) {
        if (order == null) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();

            if (meta == null) return item;

            meta.displayName(getPlugin().getLangService().menuMessage("order-menu.order-not-available"));

            item.setItemMeta(meta);

            return item;
        }

        ItemStack item = order.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        String buyerName = Bukkit.getOfflinePlayer(order.getBuyerId()).getName();
        Map<String, String> placeholders = Map.of(
                "buyer_name", buyerName != null ? buyerName : "Unknown",
                "item_name", StringUtil.formatMaterial(item),
                "item_amount", NumberParser.formatNumber(order.getAmount()),
                "price_each", NumberParser.formatNumber(order.getPricePerItem())
        );

        meta.displayName(getPlugin().getLangService().menuMessage("delivery-confirm-menu.info-button.name", placeholders));
        meta.lore(getPlugin().getLangService().menuMessageList("delivery-confirm-menu.info-button.lore", placeholders));

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack buildConfirmItem(@Nullable IOrder order) {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(getPlugin().getLangService().menuMessage("delivery-confirm-menu.confirm-button.name"));

        if (order != null) {
            // depositedItems are already validated/capped to the remaining amount by DeliveryDepositMenu.onClose,
            // so we only need to sum their quantities here
            // there is no need to recalculate validity/remaining amount.
            int deliverable = depositedItems.stream().mapToInt(ItemStack::getAmount).sum();
            BigDecimal earned = order.getPricePerItem().multiply(BigDecimal.valueOf(deliverable));

            meta.lore(getPlugin().getLangService().menuMessageList("delivery-confirm-menu.confirm-button.lore",
                    Map.of("earned", NumberParser.formatNumber(earned))));
        } else {
            meta.lore(List.of());
        }

        item.setItemMeta(meta);

        return item;
    }
}