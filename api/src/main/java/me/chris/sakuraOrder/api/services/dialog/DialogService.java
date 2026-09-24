package me.chris.sakuraOrder.api.services.dialog;

import me.chris.sakuraOrder.api.model.TextInputRequest;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Service responsible for presenting text input interfaces to players,
 * such as dialogs or sign-based input interfaces.
 *
 * <p>Implementations may use different input mechanisms depending on the
 * server version or available APIs.</p>
 *
 * <p><b>Threading contract:</b> Implementations must ensure that
 * {@link TextInputRequest#onConfirm()} and {@link TextInputRequest#onCancel()}
 * are invoked on the player's primary thread or entity thread, as appropriate
 * for the server environment. Callers are not required to perform thread
 * switching before handling these callbacks.</p>
 */
public interface DialogService {

    /**
     * Opens a text input interface for the specified player.
     *
     * @param player the player who should receive the input interface
     * @param request the input request containing the dialog content and callbacks
     */
    void openTextInput(@NotNull Player player, @NotNull TextInputRequest request);
}