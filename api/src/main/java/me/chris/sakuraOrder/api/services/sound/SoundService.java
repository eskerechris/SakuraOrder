package me.chris.sakuraOrder.api.services.sound;

import me.chris.sakuraOrder.api.model.SakuraSound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Service responsible for playing configured sounds to players.
 */
public interface SoundService {

    /**
     * Plays a sound for the specified player.
     *
     * @param player the player who should hear the sound
     * @param sound the sound to play
     */
    void play(@NotNull Player player, @NotNull SakuraSound sound);

    /**
     * Reloads the sound configuration from disk.
     */
    void reload();
}

