package me.chris.sakuraOrder.menu.framework;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class Button {

    private final int slot;
    private ItemStack item;

    public Button(int slot) {
        this.slot = slot;
    }

    public final int getSlot() {
        return slot;
    }

    @NotNull
    public final ItemStack getItem() {
        if (item == null) {
            item = createItem();
        }
        return item;
    }

    @NotNull
    protected abstract ItemStack createItem();

    public abstract void onClick(@NotNull Player player, @NotNull ClickType clickType);

    /**
     * Creates a simple interactive button backed by a fixed {@link ItemStack},
     * without needing a dedicated subclass.
     */
    @NotNull
    public static Button of(int slot, @NotNull ItemStack item, @NotNull ClickAction action) {
        return new Button(slot) {
            @Override
            @NotNull
            protected ItemStack createItem() {
                return item;
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                action.onClick(player, clickType);
            }
        };
    }

    /** Decorative item */
    @NotNull
    public static Button filler(int slot, @NotNull ItemStack item) {
        return new Button(slot) {
            @Override
            @NotNull
            protected ItemStack createItem() {
                return item;
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {}
        };
    }
}