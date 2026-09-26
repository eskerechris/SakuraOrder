package me.chris.sakuraOrder.api;

import me.chris.sakuraOrder.api.persistence.OrderRepository;
import me.chris.sakuraOrder.api.services.config.SettingsService;
import me.chris.sakuraOrder.api.services.dialog.DialogService;
import me.chris.sakuraOrder.api.services.economy.EconomyService;
import me.chris.sakuraOrder.api.services.gui.GuiService;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.api.services.material.MaterialBlacklistService;
import me.chris.sakuraOrder.api.services.order.*;
import me.chris.sakuraOrder.api.services.sound.SoundService;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Main API entry point for the SakuraOrder plugin.
 *
 * <p>Third-party plugins can use this interface to access SakuraOrder
 * services and core functionality.</p>
 *
 * @since 1.0
 */
public interface OrderPlugin extends Plugin {

    /**
     * Reloads the plugin configuration and refreshes the relevant internal state.
     */
    void reload();

    /**
     * Gets the service responsible for accessing and reloading plugin settings.
     *
     * @return the active {@link SettingsService} instance
     */
    @NotNull
    SettingsService getSettingsService();

    /**
     * Gets the service responsible for retrieving localized messages and menu content.
     *
     * @return the active {@link LangService} instance
     */
    @NotNull
    LangService getLangService();

    /**
     * Gets the service responsible for interacting with the configured economy provider.
     *
     * @return the active {@link EconomyService} instance
     */
    @NotNull
    EconomyService getEconomyService();

    /**
     * Gets the service responsible for managing the in-memory order cache.
     *
     * @return the active {@link OrderCacheService} instance
     */
    @NotNull
    OrderCacheService getOrderCacheService();

    /**
     * Gets the service responsible for managing inventory-based graphical interfaces.
     *
     * @return the active {@link GuiService} instance
     */
    @NotNull
    GuiService getGuiService();

    /**
     * Gets the service responsible for managing blacklisted materials.
     *
     * @return the active {@link MaterialBlacklistService} instance
     */
    @NotNull
    MaterialBlacklistService getMaterialBlacklistService();

    /**
     * Gets the service responsible for creating orders.
     *
     * @return the active {@link OrderCreateService} instance
     */
    @NotNull
    OrderCreateService getOrderCreateService();

    /**
     * Gets the service responsible for delivering items to orders.
     *
     * @return the active {@link OrderDeliveryService} instance
     */
    @NotNull
    OrderDeliveryService getOrderDeliveryService();

    /**
     * Gets the service responsible for finalizing active orders.
     *
     * @return the active {@link OrderCancelService} instance
     */
    @NotNull
    OrderCancelService getOrderCancelService();

    /**
     * Gets the service responsible for collecting delivered items from orders.
     *
     * @return the active {@link OrderCollectService} instance
     */
    @NotNull
    OrderCollectService getOrderCollectService();

    /**
     * Gets the service responsible for retrieving and managing player order history.
     *
     * @return the active {@link OrderHistoryService} instance
     */
    @NotNull
    OrderHistoryService getOrderHistoryService();

    /**
     * Gets the repository responsible for persisting and querying orders.
     *
     * @return the active {@link OrderRepository} instance
     */
    @NotNull
    OrderRepository getOrderRepository();

    /**
     * Gets the service responsible for opening player text input dialogs.
     *
     * @return the active {@link DialogService} instance
     */
    @NotNull
    DialogService getDialogService();

    /**
     * Gets the service responsible for playing sound to player.
     *
     * @return the active {@link SoundService} instance
     */
    @NotNull
    SoundService getSoundService();
}