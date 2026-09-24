package me.chris.sakuraOrder.api.services.lang;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Service providing localized, color-formatted messages for two separate
 * domains: general system messages and menu (GUI) content.
 */
public interface LangService {

    @NotNull
    Component message(@NotNull String key);

    @NotNull
    Component message(@NotNull String key, @NotNull Map<String, String> placeholders);

    @NotNull
    List<Component> messageList(@NotNull String key);

    @NotNull
    List<Component> messageList(@NotNull String key, @NotNull Map<String, String> placeholders);

    @NotNull
    Component menuMessage(@NotNull String key);

    @NotNull
    Component menuMessage(@NotNull String key, @NotNull Map<String, String> placeholders);

    @NotNull
    List<Component> menuMessageList(@NotNull String key);

    @NotNull
    List<Component> menuMessageList(@NotNull String key, @NotNull Map<String, String> placeholders);

    /**
     * Reloads translation files from disk, re-copying any missing bundled locales first.
     */
    void reload();
}