package me.chris.sakuraOrder.services.sound;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.api.services.sound.SoundService;
import me.chris.sakuraOrder.services.scheduler.SchedulerService;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages configured event sounds, backed by {@code sound.yml}.
 */
public class PluginSoundService implements SoundService {

    private static final float VOLUME = 1.0f;
    private static final float PITCH = 1.0f;

    private final OrderPlugin plugin;
    private final SchedulerService scheduler;
    private final File file;

    private volatile Map<SakuraSound, Sound> sounds = new EnumMap<>(SakuraSound.class);

    public PluginSoundService(@NotNull OrderPlugin plugin, @NotNull SchedulerService scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.file = new File(plugin.getDataFolder(), "sound.yml");
        reload();
    }

    @Override
    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("sound.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<SakuraSound, Sound> parsed = new EnumMap<>(SakuraSound.class);

        for (SakuraSound key : SakuraSound.values()) {
            ConfigurationSection section = config.getConfigurationSection(key.configKey());
            if (section == null) {
                plugin.getLogger().log(Level.WARNING,
                        "sound.yml: missing entry for '" + key.configKey() + "', sound disabled");
                continue;
            }

            if (!section.getBoolean("enabled", true)) {
                continue;
            }

            String raw = section.getString("sound");
            if (raw == null || raw.isBlank()) {
                plugin.getLogger().log(Level.WARNING,
                        "sound.yml: missing 'sound' for '" + key.configKey() + "', ignored");
                continue;
            }

            NamespacedKey soundKey = NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));
            Sound sound = soundKey != null ? Registry.SOUNDS.get(soundKey) : null;
            if (sound == null) {
                plugin.getLogger().log(Level.WARNING,
                        "sound.yml: unknown sound '" + raw + "' for '" + key.configKey() + "', ignored");
                continue;
            }

            parsed.put(key, sound);
        }

        this.sounds = parsed;
    }

    @Override
    public void play(@NotNull Player player, @NotNull SakuraSound sound) {
        Sound bukkitSound = sounds.get(sound);
        if (bukkitSound == null) return;

        scheduler.runNowOrAtEntity(player,
                () -> player.playSound(player.getLocation(), bukkitSound, VOLUME, PITCH));
    }
}