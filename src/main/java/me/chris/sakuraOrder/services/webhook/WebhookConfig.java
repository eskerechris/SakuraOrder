package me.chris.sakuraOrder.services.webhook;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *  A reload builds a new instance.
 */
public final class WebhookConfig {

    private static final List<String> HOSTS = List.of("discord.com", "discordapp.com", "canary.discord.com", "ptb.discord.com");
    private static final Map<String, Integer> DEFAULT_COLORS = Map.of(
            "order-create", 0x57F287,
            "order-delivery", 0x5865F2,
            "order-collect", 0xFEE75C,
            "order-cancel", 0xED4245
    );

    /** Resolved settings of one event. {@code url} is null when the event has no valid URL. */
    public record EventSettings(@Nullable URI url, int color) {}

    private final boolean enabled;
    private final Map<String, EventSettings> events;

    private WebhookConfig(boolean enabled, @NotNull Map<String, EventSettings> events) {
        this.enabled = enabled;
        this.events = events;
    }

    /** Initial state before the first load. */
    public static @NotNull WebhookConfig disabled() {
        return new WebhookConfig(false, Map.of());
    }

    public boolean enabled() {
        return enabled;
    }

    /** @return settings for the event key, or null when the event is disabled / misconfigured. */
    public @Nullable EventSettings event(@NotNull String key) {
        EventSettings settings = events.get(key);
        return settings == null || settings.url() == null ? null : settings;
    }

    public static @NotNull WebhookConfig load(@NotNull FileConfiguration cfg, @NotNull Logger logger) {
        boolean enabled = cfg.getBoolean("enabled", false);
        String defaultUrl = cfg.getString("url", "");
        ConfigurationSection section = cfg.getConfigurationSection("events");

        Map<String, EventSettings> parsed = new HashMap<>();
        for (Map.Entry<String, Integer> def : DEFAULT_COLORS.entrySet()) {
            String key = def.getKey();
            ConfigurationSection ev = section == null ? null : section.getConfigurationSection(key);
            if (ev == null || !ev.getBoolean("enabled", true)) {
                continue;
            }
            String raw = ev.getString("url", "");
            URI uri = validate(raw.isBlank() ? defaultUrl : raw);
            if (uri == null && enabled) {
                logger.warning("(Webhook) Event '" + key + "' has no valid Discord webhook URL and will be ignored.");
            }
            parsed.put(key, new EventSettings(uri, parseColor(ev.getString("color"), def.getValue())));
        }
        return new WebhookConfig(enabled, Map.copyOf(parsed));
    }

    private static @Nullable URI validate(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || HOSTS.stream().noneMatch(h -> h.equalsIgnoreCase(uri.getHost()))
                    || uri.getPath() == null || !uri.getPath().startsWith("/api/webhooks/")) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int parseColor(@Nullable String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}