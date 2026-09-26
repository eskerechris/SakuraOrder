package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Next/prev page navigation button
 */
public class NavigationButton extends Button {

    private final OrderPlugin plugin;
    private final boolean isNextButton;
    private final boolean hasPage;
    private final Runnable action;

    public NavigationButton(OrderPlugin plugin, int slot, boolean isNextButton, boolean hasPage, @NotNull Runnable action) {
        super(slot);
        this.plugin = plugin;
        this.hasPage = hasPage;
        this.isNextButton = isNextButton;
        this.action = action;
    }

    @Override
    @NotNull
    protected ItemStack createItem() {
        var item = new ItemStack(Material.ARROW);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        if (isNextButton) {
            meta.displayName(plugin.getLangService().menuMessage("paginated-menu.next-page-button.name"));
            meta.lore(plugin.getLangService().menuMessageList("paginated-menu.next-page-button.lore"));
            item.setItemMeta(meta);
            return item;
        }

        meta.displayName(plugin.getLangService().menuMessage("paginated-menu.prev-page-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("paginated-menu.prev-page-button.lore"));
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        if (hasPage) {
            action.run();
            plugin.getSoundService().play(player, SakuraSound.PAGE_CHANGE);
        }
    }
}
