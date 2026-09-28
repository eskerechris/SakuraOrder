package me.chris.sakuraOrder.model;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * Represents a typed Discord webhook payload serialized directly by Gson.
 */
public record WebhookPayload(
        @Nullable String username,
        @SerializedName("allowed_mentions") @NotNull AllowedMentions allowedMentions,
        @NotNull List<Embed> embeds
) {

    public static @NotNull WebhookPayload of(
            @NotNull String title,
            int color,
            @NotNull List<Field> fields
    ) {
        return of(title, color, null, fields);
    }

    public static @NotNull WebhookPayload of(
            @NotNull String title,
            int color,
            @Nullable String avatarUrl,
            @NotNull List<Field> fields
    ) {
        Thumbnail thumbnail = avatarUrl != null && !avatarUrl.isBlank() ? new Thumbnail(avatarUrl) : null;
        // username left null discord then falls back to the name/avatar
        // configured on the webhook itself
        return new WebhookPayload(
                null,
                AllowedMentions.NONE,
                List.of(new Embed(title, color, Instant.now().toString(), thumbnail, fields)));
    }

    /**
     * Represents the Discord mention configuration for a webhook payload.
     *
     * @param parse the types of mentions that Discord is allowed to parse
     */
    public record AllowedMentions(@NotNull List<String> parse) {

        /**
         * A mention configuration that disables all Discord mentions.
         */
        public static final AllowedMentions NONE = new AllowedMentions(List.of());
    }

    /** Represents the URL of the player's 100px avatar. */
    public record Thumbnail(@NotNull String url) {}

    /**
     * Represents a Discord embed contained in a webhook payload.
     *
     * @param title the title of the embed, limited to 256 characters
     * @param color the color of the embed
     * @param timestamp the timestamp associated with the embed
     * @param fields the fields contained in the embed
     */
    public record Embed(
            @NotNull String title,
            int color,
            @NotNull String timestamp,
            @Nullable Thumbnail thumbnail,
            @NotNull List<Field> fields
    ) {
        public Embed {
            title = truncate(title, 256);
        }

        public Embed(
                @NotNull String title,
                int color,
                @NotNull String timestamp,
                @NotNull List<Field> fields
        ) {
            this(title, color, timestamp, null, fields);
        }
    }

    /**
     * Represents a field within a Discord embed.
     *
     * @param name the field name, limited to 256 characters
     * @param value the field value, limited to 1024 characters
     * @param inline whether the field should be displayed inline
     */
    public record Field(@NotNull String name, @NotNull String value, boolean inline) {

        public Field {
            name = truncate(name, 256);
            value = truncate(value, 1024);
        }

        /**
         * Creates an inline field.
         *
         * @param name the field name
         * @param value the field value
         */
        public Field(@NotNull String name, @NotNull String value) {
            this(name, value, true);
        }
    }

    /**
     * Truncates a string to the specified maximum length.
     *
     * @param value the string to truncate
     * @param max the maximum length of the resulting string
     * @return the original string if it fits within the limit, otherwise a truncated string
     */
    private static @NotNull String truncate(@NotNull String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}