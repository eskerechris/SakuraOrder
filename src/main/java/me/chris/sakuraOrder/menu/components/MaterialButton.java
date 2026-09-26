package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.OrderableItem;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.menu.MaterialSelectMenu;
import me.chris.sakuraOrder.menu.framework.Button;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;

public class MaterialButton extends Button {

    private final OrderPlugin plugin;
    private final LangService lang;
    private final OrderableItem currentItem;
    private final BiConsumer<Player, OrderableItem> onItemSet;

    public MaterialButton(OrderPlugin plugin, int slot, @Nullable OrderableItem currentItem,
                          @NotNull BiConsumer<Player, OrderableItem> onItemSet) {
        super(slot);
        this.plugin = plugin;
        this.lang = plugin.getLangService();
        this.currentItem = currentItem;
        this.onItemSet = onItemSet;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        ItemStack item = currentItem != null ? currentItem.itemStack().clone() : new ItemStack(Material.STONE);
        var meta = item.getItemMeta();
        if (meta == null) return item;

        String material = currentItem != null
                ? currentItem.displayName()
                : PlainTextComponentSerializer.plainText().serialize(lang.menuMessage("new-order-menu.not-set"));

        meta.displayName(lang.menuMessage("new-order-menu.material-button.name"));
        meta.lore(lang.menuMessageList("new-order-menu.material-button.lore", Map.of("material", material)));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        new MaterialSelectMenu(plugin, onItemSet).displayTo(player);
        plugin.getSoundService().play(player, SakuraSound.BUTTON_INTERACT);
    }
}