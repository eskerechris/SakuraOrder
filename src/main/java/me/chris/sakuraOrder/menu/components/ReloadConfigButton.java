package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.menu.framework.Button;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ReloadConfigButton extends Button {

    private final OrderPlugin plugin;

    public ReloadConfigButton(int slot, OrderPlugin plugin) {
        super(slot);
        this.plugin = plugin;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.COMMAND_BLOCK);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(plugin.getLangService().menuMessage("admin-panel-menu.reload-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("admin-panel-menu.reload-button.lore"));

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        player.closeInventory();
        var start = System.currentTimeMillis();
        plugin.reload();
        var end = System.currentTimeMillis();

        player.sendMessage(Component.text("SakuraOrder configuration reloaded successfully in %dms".formatted(end - start),
                NamedTextColor.GREEN));
        plugin.getSoundService().play(player, SakuraSound.BUTTON_INTERACT);
    }
}
