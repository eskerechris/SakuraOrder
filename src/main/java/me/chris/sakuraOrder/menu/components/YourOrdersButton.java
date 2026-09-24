package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.menu.YourOrdersMenu;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class YourOrdersButton extends Button {

    private final OrderPlugin plugin;

    public YourOrdersButton(OrderPlugin plugin, int slot) {
        super(slot);
        this.plugin = plugin;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.CHEST);
        var meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(plugin.getLangService().menuMessage("order-menu.your-orders-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("order-menu.your-orders-button.lore"));

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        new YourOrdersMenu(plugin).displayTo(player);
    }
}
