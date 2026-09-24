package me.chris.sakuraOrder.util;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Formats a remaining {@link Duration} into a compact string showing at most
 * 3 time units, dropping higher-order units once they hit zero.
 */
public final class TimeFormatter {

    private TimeFormatter() {}

    @NotNull
    public static String formatRemaining(@NotNull Duration remaining) {
        if (remaining.isNegative() || remaining.isZero()) {
            return "Expired";
        }

        long totalSeconds = remaining.getSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%dg %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}