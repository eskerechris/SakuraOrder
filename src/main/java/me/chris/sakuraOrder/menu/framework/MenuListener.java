package me.chris.sakuraOrder.menu.framework;

import me.chris.sakuraOrder.menu.DeliveryDepositMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Global listener handling inventory clicks and close events for all {@link Menu} instances.
 */
public class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) {
            return;
        }

        if (!(menu instanceof DeliveryDepositMenu)) event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getInventory()) {
            return;
        }

        int slot = event.getSlot();
        if (slot < 0 || slot >= menu.getSize()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Button button = menu.getButton(slot);
        if (button != null) {
            button.onClick(player, event.getClick());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            menu.onClose(player);
        }
    }
}