package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.StringUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class HistoryOrderButton extends Button {

    private final IOrder order;
    private final OrderPlugin plugin;

    public HistoryOrderButton(int slot, @NotNull IOrder order, @NotNull OrderPlugin plugin) {
        super(slot);
        this.order = order;
        this.plugin = plugin;
    }

    @Override
    @NotNull
    protected ItemStack createItem() {
        var item = order.getItemStack();
        var meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        Map<String, String> placeholders = Map.of(
                "item_amount", NumberParser.formatNumber(order.getOriginalAmount()),
                "item_name", StringUtil.formatMaterial(order.getItemStack()),
                "price_each", NumberParser.formatNumber(order.getPricePerItem()),
                "delivered", NumberParser.formatNumber(order.getDelivered()),
                "collected", NumberParser.formatNumber(order.getCollected()),
                "total_price", NumberParser.formatNumber(order.getTotalPrice()),
                "status", order.getOrderStatus().name()
        );
        meta.displayName(plugin.getLangService().menuMessage(
                "player-order-history-menu.order-button.name", placeholders));
        meta.lore(plugin.getLangService().menuMessageList(
                "player-order-history-menu.order-button.lore", placeholders));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {}
}
