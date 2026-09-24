package me.chris.sakuraOrder.menu.framework;

import me.chris.sakuraOrder.api.OrderPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base abstract class representing an Inventory GUI Menu.
 */
public abstract class Menu implements InventoryHolder {

    private final OrderPlugin plugin;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private @Nullable Player viewer;
    private boolean reopening = false;

    private int size = 9 * 3;
    private Component title = Component.text("Menu");

    private final @Nullable Menu parent;
    private Inventory inventory;

    public Menu(@NotNull OrderPlugin plugin) {
        this(plugin, null);
    }

    public Menu(@NotNull OrderPlugin plugin, @Nullable Menu parent) {
        this.plugin = plugin;
        this.parent = parent;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @NotNull
    protected final OrderPlugin getPlugin() {
        return plugin;
    }

    @Nullable
    public final Menu getParent() {
        return parent;
    }

    /**
     * Returns an immutable snapshot of the currently registered buttons.
     */
    @NotNull
    public final List<Button> getButtons() {
        return List.copyOf(buttons.values());
    }

    @Nullable
    public final Button getButton(int slot) {
        return buttons.get(slot);
    }

    /** Overrides any button already present */
    protected final void addButton(@NotNull Button button) {
        buttons.put(button.getSlot(), button);
    }

    protected final void clearButtons() {
        buttons.clear();
    }

    protected final void setSize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Menu size is invalid: " + size + ". Must be a multiple of 9 between 9 and 54.");
        }
        this.size = size;
    }

    public final int getSize() {
        return size;
    }

    @NotNull
    public final Component getTitle() {
        return title;
    }

    protected final void setTitle(@NotNull Component title) {
        this.title = title;
    }

    protected final void setTitle(@NotNull String title) {
        this.title = Component.text(title);
    }

    public final @Nullable Player getViewer() {
        return viewer;
    }

    protected abstract void registerButtons();

    protected void updateInventory() {
        if (inventory == null) {
            return;
        }
        for (Button button : buttons.values()) {
            if (button.getSlot() >= 0 && button.getSlot() < size) {
                inventory.setItem(button.getSlot(), button.getItem());
            }
        }
    }

    protected void updateDynamicButtons() {
        for (Button button : buttons.values()) {
            if (button instanceof DynamicButton dynamicButton) {
                dynamicButton.update();
            }
        }
    }

    public void displayTo(@NotNull Player player) {
        this.viewer = player;
        clearButtons();
        registerButtons();

        inventory = Bukkit.createInventory(this, size, title);
        updateInventory();

        player.openInventory(inventory);
    }

    protected final void reopenForTitleChange() {
        if (viewer == null) {
            return;
        }
        reopening = true;
        inventory = Bukkit.createInventory(this, size, title);
        updateInventory();
        viewer.openInventory(inventory);
        reopening = false;
    }

    public void onClose(@NotNull Player player) {
        if (reopening) {
            return; // ignore close events triggered by reopening the inventory
        }
        if (viewer == player) {
            viewer = null;
        }
    }
}