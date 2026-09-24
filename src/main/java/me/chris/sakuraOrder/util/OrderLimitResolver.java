package me.chris.sakuraOrder.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class OrderLimitResolver {

    public static final String PERMISSION_PREFIX = "sakuraorder.limit.";
    public static final int MIN_LIMIT = 0;
    public static final int MAX_LIMIT = 26;

    private OrderLimitResolver() {}

    /**
     * Returns the player’s order limit based on the `order.limit.N` permissions
     * (where N is between 0 and 26), or the fallback value if the player has no such permissions.
     */
    public static int resolve(@NotNull Player player, int fallback) {
        // From highest to lowest: the highest limit amongst those held wins
        for (int i = MAX_LIMIT; i >= MIN_LIMIT; i--) {
            if (player.hasPermission(PERMISSION_PREFIX + i)) {
                return i;
            }
        }
        return fallback;
    }
}