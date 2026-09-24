package me.chris.sakuraOrder.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses raw lang strings into Adventure {@link Component}s.
 * Supports legacy '&' color/format codes and standard #HEX / &#HEX color formats.
 */
public final class ColorParser {

    // Matches both #RRGGBB and &#RRGGBB without duplicating the '&'
    private static final Pattern HEX_PATTERN = Pattern.compile("&?#([A-Fa-f0-9]{6})");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private ColorParser() {}

    @NotNull
    public static Component parse(@NotNull String raw, @NotNull Map<String, String> placeholders) {
        // Substitute placeholders
        String text = applyPlaceholders(raw, placeholders);

        // Transpile #HEX codes into the format compatible with LegacyComponentSerializer (&#RRGGBB)
        String convertedText = HEX_PATTERN.matcher(text).replaceAll("&#$1");

        // Deserialize the entire string (containing & and &#HEX) into an Adventure Component
        Component result = LEGACY.deserialize(convertedText);

        // Disable default client-side italics for items/GUIs
        return result.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    @NotNull
    private static String applyPlaceholders(@NotNull String raw, @NotNull Map<String, String> placeholders) {
        if (placeholders.isEmpty()) {
            return raw;
        }

        String result = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}