package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.util.OrderMaintenanceLock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OrderLockButton extends Button {

    private final OrderPlugin plugin;

    public OrderLockButton(int slot, OrderPlugin plugin) {
        super(slot);
        this.plugin = plugin;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        Material material;
        if (OrderMaintenanceLock.isLocked()) {
            material = Material.RED_CONCRETE;
        } else {
            material = Material.GREEN_CONCRETE;
        }

        var item = new ItemStack(material);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(plugin.getLangService().menuMessage("admin-panel-menu.lock-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("admin-panel-menu.lock-button.lore"));

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        if (OrderMaintenanceLock.isLocked()) {
            player.closeInventory();
            OrderMaintenanceLock.unlock();
            player.sendMessage(Component.text("Order services have been unlocked", NamedTextColor.GREEN));
        } else {
            player.closeInventory();
            OrderMaintenanceLock.lock();
            player.sendMessage(Component.text("Order services have been locked", NamedTextColor.GREEN));
        }
        plugin.getSoundService().play(player, SakuraSound.BUTTON_INTERACT);
    }
}
