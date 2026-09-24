package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.menu.NewOrderMenu;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CreateOrderButton extends Button {

    private final OrderPlugin plugin;
    private final boolean canCreate;

    public CreateOrderButton(OrderPlugin plugin, int slot, boolean canCreate) {
        super(slot);
        this.plugin = plugin;
        this.canCreate = canCreate;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.MAP);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        if (canCreate) {
            meta.displayName(plugin.getLangService().menuMessage("your-orders-menu.create-order-button.name"));
            meta.lore(plugin.getLangService().menuMessageList("your-orders-menu.create-order-button.lore"));
        } else {
            meta.displayName(plugin.getLangService().menuMessage("your-orders-menu.create-order-button.name-limit"));
            meta.lore(plugin.getLangService().menuMessageList("your-orders-menu.create-order-button.lore-limit"));
        }

        item.setItemMeta(meta);
        return item;
    }


    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        if (!canCreate) return;

        new NewOrderMenu(plugin).displayTo(player);

    }
}
