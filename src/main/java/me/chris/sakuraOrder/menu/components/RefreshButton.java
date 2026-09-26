package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RefreshButton extends Button {

    private final OrderPlugin plugin;
    private final Runnable onRefresh;

    public RefreshButton(OrderPlugin plugin, int slot, @NotNull Runnable onRefresh) {
        super(slot);
        this.plugin = plugin;
        this.onRefresh = onRefresh;
    }

    @Override
    @NotNull
    protected ItemStack createItem() {
        var item = new ItemStack(Material.MAP);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLangService().menuMessage("order-menu.refresh-button.name"));
            meta.lore(plugin.getLangService().menuMessageList("order-menu.refresh-button.lore"));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        onRefresh.run();
        plugin.getSoundService().play(player, SakuraSound.BUTTON_INTERACT);
    }
}