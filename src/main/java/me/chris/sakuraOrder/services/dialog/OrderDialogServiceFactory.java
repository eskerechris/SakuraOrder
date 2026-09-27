package me.chris.sakuraOrder.services.dialog;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.dialog.DialogService;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the appropriate {@link DialogService} implementation.
 *
 * <p>Uses runtime class detection to avoid loading Paper's Dialog API on
 * unsupported server versions.</p>
 */
public final class OrderDialogServiceFactory {

    private OrderDialogServiceFactory() {}

    /**
     * Creates the active {@link DialogService} implementation.
     *
     * @param plugin the plugin instance
     * @return the active dialog service
     */
    @NotNull
    public static DialogService create(@NotNull OrderPlugin plugin) {
        try {
            Class.forName("io.papermc.paper.dialog.Dialog");
            return (DialogService) Class.forName(
                    "me.chris.sakuraOrder.services.dialog.PaperDialogService"
            ).getDeclaredConstructor(OrderPlugin.class).newInstance(plugin);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Paper Dialog API unavailable, falling back to chat-based dialog input");
            return new FallBackDialogService(plugin);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().severe("Failed to construct PaperDialogService, falling back to chat-based dialog input");
            return new FallBackDialogService(plugin);
        }
    }
}