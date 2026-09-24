package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.menu.CollectMenu;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CollectButton extends Button {

    private final OrderPlugin plugin;
    private final UUID orderId;

    public CollectButton(int slot, UUID orderId, OrderPlugin plugin) {
        super(slot);
        this.plugin = plugin;
        this.orderId = orderId;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.CHEST);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(plugin.getLangService().menuMessage("edit-order-menu.collect-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("edit-order-menu.collect-button.lore"));

        item.setItemMeta(meta);

        return item;
    }


    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        if (plugin.getOrderCacheService().get(orderId) == null) {
            return;
        }

        new CollectMenu(plugin, orderId).displayTo(player);
    }
}
