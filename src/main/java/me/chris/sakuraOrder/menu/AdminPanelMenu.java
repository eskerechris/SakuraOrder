package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.menu.components.OrderLockButton;
import me.chris.sakuraOrder.menu.components.ReloadConfigButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.Menu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AdminPanelMenu extends Menu {

    private final static int RELOAD_CONFIG_BUTTON_SLOT = 12;
    private final static int ORDER_LOCK_BUTTON_SLOT = 14;

    public AdminPanelMenu(OrderPlugin plugin) {
        super(plugin);
        this.setTitle(plugin.getLangService().menuMessage("admin-panel-menu.title"));
        this.setSize(9 * 3);
    }

    @Override
    protected void registerButtons() {
        for (int slot = 0; slot < getSize(); slot++) {
            addButton(Button.filler(slot, createFiller()));
        }

        addButton(new ReloadConfigButton(RELOAD_CONFIG_BUTTON_SLOT, getPlugin()));
        addButton(new OrderLockButton(ORDER_LOCK_BUTTON_SLOT, getPlugin()));
    }

    private ItemStack createFiller() {
        var item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(Component.empty());

        item.setItemMeta(meta);

        return item;
    }

}
