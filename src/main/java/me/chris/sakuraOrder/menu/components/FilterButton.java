package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.menu.framework.Button;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public class FilterButton<E extends Enum<E>> extends Button {

    private static final String SQUARE = "▪ ";

    private final OrderPlugin plugin;
    private final Supplier<E> currentFilter;
    private final Consumer<E> onFilterChanged;

    public FilterButton(@NotNull OrderPlugin plugin, int slot,
                        @NotNull Supplier<E> currentFilter,
                        @NotNull Consumer<E> onFilterChanged) {
        super(slot);
        this.plugin = plugin;
        this.currentFilter = currentFilter;
        this.onFilterChanged = onFilterChanged;
    }

    @Override
    @NotNull
    protected ItemStack createItem() {
        E selected = currentFilter.get();
        TextColor selectedColor = resolveColor("paginated-menu.filter-button.selected-color", NamedTextColor.RED);
        TextColor unselectedColor = resolveColor("paginated-menu.filter-button.unselected-color", NamedTextColor.WHITE);

        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLangService().menuMessage("paginated-menu.filter-button.name"));

            List<Component> lore = new ArrayList<>();
            for (E filter : values(selected)) {
                TextColor color = (filter == selected) ? selectedColor : unselectedColor;
                lore.add(filterLine(filter, color));
            }

            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    /** All constants of E, discovered from a live instance - no Class<E> parameter needed. */
    @NotNull
    private E[] values(@NotNull E anyInstance) {
        return anyInstance.getDeclaringClass().getEnumConstants();
    }

    @NotNull
    private TextColor resolveColor(@NotNull String key, @NotNull TextColor fallback) {
        TextColor color = firstColor(plugin.getLangService().menuMessage(key));
        if (color == null) {
            plugin.getLogger().log(Level.WARNING,
                    "Lang key '" + key + "' has no resolvable color (expected e.g. '&c' or '#FF0000'), using fallback");
            return fallback;
        }
        return color;
    }

    @Nullable
    private TextColor firstColor(@NotNull Component component) {
        if (component.color() != null) {
            return component.color();
        }
        for (Component child : component.children()) {
            TextColor color = firstColor(child);
            if (color != null) {
                return color;
            }
        }
        return null;
    }

    @NotNull
    private Component filterLine(@NotNull E filter, @NotNull TextColor color) {
        Component label = plugin.getLangService()
                .menuMessage("paginated-menu.filter-button." + filter.name().toLowerCase());

        return Component.text(SQUARE, color)
                .append(label.color(color))
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        E current = currentFilter.get();
        E[] values = values(current);
        int newIndex = clickType.isRightClick()
                ? (current.ordinal() - 1 + values.length) % values.length
                : (current.ordinal() + 1) % values.length;
        onFilterChanged.accept(values[newIndex]);
        plugin.getSoundService().play(player, SakuraSound.BUTTON_INTERACT);
    }
}