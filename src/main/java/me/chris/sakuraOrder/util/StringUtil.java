package me.chris.sakuraOrder.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for handling string formatting and text operations.
 */
public final class StringUtil {

    private StringUtil() {
    }

    /**
     * Formats the material name of the given ItemStack into a human-readable title.
     * Example: DIAMOND_SWORD -> "Diamond Sword", BONE_MEAL -> "Bone Meal".
     *
     * @param item the ItemStack to get the formatted name from
     * @return the formatted material name, or an empty string if the item is null/air
     */
    public static String formatMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }

        String rawName = item.getType().name();
        String[] words = rawName.toLowerCase().split("_");
        StringBuilder formattedName = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                formattedName.append(Character.toUpperCase(words[i].charAt(0)))
                        .append(words[i].substring(1));
                if (i < words.length - 1) {
                    formattedName.append(" ");
                }
            }
        }

        return formattedName.toString();
    }

    public static String formatMaterial(Material item) {
        if (item == null || item.isAir()) {
            return "";
        }

        String rawName = item.name();
        String[] words = rawName.toLowerCase().split("_");
        StringBuilder formattedName = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                formattedName.append(Character.toUpperCase(words[i].charAt(0)))
                        .append(words[i].substring(1));
                if (i < words.length - 1) {
                    formattedName.append(" ");
                }
            }
        }

        return formattedName.toString();
    }
}