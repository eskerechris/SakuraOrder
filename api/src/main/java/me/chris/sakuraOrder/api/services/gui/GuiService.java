package me.chris.sakuraOrder.api.services.gui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Provides access to the plugin main Gui.
 */
public interface GuiService {

    /**
     * Opens the order menu for the specified player.
     *
     * @param player the player for whom the order menu should be opened
     */
    void openOrderMenu(@NotNull Player player);

    /**
     * Opens the player's orders menu.
     *
     * @param player the player for whom the orders menu should be opened
     */
    void openYoursOrderMenu(@NotNull Player player);

    /**
     * Opens the order editing menu for the specified order.
     *
     * @param player the player for whom the edit menu should be opened
     * @param orderId the unique identifier of the order to edit
     */
    void openEditOrderMenu(@NotNull Player player, @NotNull UUID orderId);

}
