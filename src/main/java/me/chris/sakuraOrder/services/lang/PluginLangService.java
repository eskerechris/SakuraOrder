package me.chris.sakuraOrder.services.lang;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.util.ColorParser;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class PluginLangService implements LangService {

    /** Locales bundled with the plugin jar under resources/lang and resources/lang/menu. */
    private static final List<String> BUNDLED_LOCALES = List.of("it_IT", "en_GB");

    private final OrderPlugin plugin;
    private volatile LangHolder holder;

    public PluginLangService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void reload() {
        saveBundledLocales();

        String activeLocale = plugin.getSettingsService().getGeneral().getLanguage();
        String fallbackLocale = plugin.getSettingsService().getGeneral().getFallbackLanguage();

        FileConfiguration messages = loadLocaleFile("lang/" + activeLocale + ".yml");
        FileConfiguration menuMessages = loadLocaleFile("lang/menu/" + activeLocale + ".yml");

        FileConfiguration fallbackMessages;
        FileConfiguration fallbackMenuMessages;

        if (fallbackLocale.equals(activeLocale)) {
            fallbackMessages = messages;
            fallbackMenuMessages = menuMessages;
        } else {
            fallbackMessages = loadLocaleFile("lang/" + fallbackLocale + ".yml");
            fallbackMenuMessages = loadLocaleFile("lang/menu/" + fallbackLocale + ".yml");
        }

        // Atomic swap
        this.holder = new LangHolder(messages, menuMessages, fallbackMessages, fallbackMenuMessages,
                activeLocale, fallbackLocale);
    }

    @Override
    @NotNull
    public Component message(@NotNull String key) {
        return message(key, Map.of());
    }

    @Override
    @NotNull
    public Component message(@NotNull String key, @NotNull Map<String, String> placeholders) {
        LangHolder h = this.holder;
        return resolve(key, placeholders, h.messages(), h.fallbackMessages(), h.activeLocale(), h.fallbackLocale());
    }

    @Override
    @NotNull
    public List<Component> messageList(@NotNull String key) {
        return messageList(key, Map.of());
    }

    @Override
    @NotNull
    public List<Component> messageList(@NotNull String key, @NotNull Map<String, String> placeholders) {
        LangHolder h = this.holder;
        return resolveList(key, placeholders, h.messages(), h.fallbackMessages(), h.activeLocale(), h.fallbackLocale());
    }

    @Override
    @NotNull
    public List<Component> menuMessageList(@NotNull String key) {
        return menuMessageList(key, Map.of());
    }

    @Override
    @NotNull
    public List<Component> menuMessageList(@NotNull String key, @NotNull Map<String, String> placeholders) {
        LangHolder h = this.holder;
        return resolveList(key, placeholders, h.menuMessages(), h.fallbackMenuMessages(), h.activeLocale(), h.fallbackLocale());
    }

    @Override
    @NotNull
    public Component menuMessage(@NotNull String key) {
        return menuMessage(key, Map.of());
    }

    @Override
    @NotNull
    public Component menuMessage(@NotNull String key, @NotNull Map<String, String> placeholders) {
        LangHolder h = this.holder;
        return resolve(key, placeholders, h.menuMessages(), h.fallbackMenuMessages(), h.activeLocale(), h.fallbackLocale());
    }

    @NotNull
    private Component resolve(@NotNull String key, @NotNull Map<String, String> placeholders,
                              @NotNull FileConfiguration active, @NotNull FileConfiguration fallback,
                              @NotNull String activeLocale, @NotNull String fallbackLocale) {
        String raw = active.getString(key);

        if (raw == null) {
            raw = fallback.getString(key);
            if (raw == null) {
                plugin.getLogger().log(Level.WARNING,
                        "Missing lang key '" + key + "' in both '" + activeLocale + "' and fallback '" + fallbackLocale + "'");
                return Component.text(key);
            }
            plugin.getLogger().log(Level.WARNING,
                    "Missing lang key '" + key + "' in '" + activeLocale + "', using fallback '" + fallbackLocale + "'");
        }

        return ColorParser.parse(raw, placeholders);
    }

    @NotNull
    private List<Component> resolveList(@NotNull String key, @NotNull Map<String, String> placeholders,
                                        @NotNull FileConfiguration active, @NotNull FileConfiguration fallback,
                                        @NotNull String activeLocale, @NotNull String fallbackLocale) {
        List<String> rawList = active.getStringList(key);

        if (rawList.isEmpty()) {
            rawList = fallback.getStringList(key);
            if (rawList.isEmpty()) {
                plugin.getLogger().log(Level.WARNING,
                        "Missing lang list key '" + key + "' in both '" + activeLocale + "' and fallback '" + fallbackLocale + "'");
                return List.of(Component.text(key));
            }
        }

        return rawList.stream()
                .map(line -> ColorParser.parse(line, placeholders))
                .toList();
    }

    private void saveBundledLocales() {
        for (String locale : BUNDLED_LOCALES) {
            saveResourceSafely("lang/" + locale + ".yml");
            saveResourceSafely("lang/menu/" + locale + ".yml");
        }
    }

    private void saveResourceSafely(@NotNull String resourcePath) {
        try {
            plugin.saveResource(resourcePath, false);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Bundled lang resource not found in jar: " + resourcePath, e);
        }
    }

    @NotNull
    private FileConfiguration loadLocaleFile(@NotNull String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);

        if (!file.exists()) {
            plugin.getLogger().log(Level.WARNING,
                    "Lang file not found: " + relativePath + " — using an empty configuration for it");
            return new YamlConfiguration();
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    private record LangHolder(
            FileConfiguration messages,
            FileConfiguration menuMessages,
            FileConfiguration fallbackMessages,
            FileConfiguration fallbackMenuMessages,
            String activeLocale,
            String fallbackLocale
    ) {}
}