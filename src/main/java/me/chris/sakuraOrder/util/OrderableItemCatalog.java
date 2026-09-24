package me.chris.sakuraOrder.util;

import me.chris.sakuraOrder.api.model.OrderableItem;
import me.chris.sakuraOrder.api.services.material.MaterialBlacklistService;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class OrderableItemCatalog {

    private static final Set<String> NON_SURVIVAL_POTION_TYPES = Set.of(
            "WATER", "MUNDANE", "THICK", "AWKWARD", "UNCRAFTABLE", "LUCK"
    );

    private OrderableItemCatalog() {
    }

    @NotNull
    public static List<OrderableItem> build(@NotNull MaterialBlacklistService blacklist) {
        List<OrderableItem> result = new ArrayList<>();

        for (Material material : Material.values()) {
            if (!material.isItem() || material.isLegacy()) continue;
            if (blacklist.isBlacklisted(material)) continue;

            switch (material) {
                case POTION, SPLASH_POTION, LINGERING_POTION, TIPPED_ARROW -> addPotionVariants(result, material, blacklist);
                case ENCHANTED_BOOK -> addEnchantedBookVariants(result, blacklist);
                default -> result.add(new OrderableItem(new ItemStack(material), material, formatEnumName(material.name())));
            }
        }

        return result;
    }

    private static void addPotionVariants(@NotNull List<OrderableItem> result, @NotNull Material potionMaterial,
                                          @NotNull MaterialBlacklistService blacklist) {
        for (PotionType potionType : PotionType.values()) {
            if (NON_SURVIVAL_POTION_TYPES.contains(potionType.name())) continue;
            if (blacklist.isVariantBlacklisted(potionMaterial, potionType.name(), null)) continue;

            ItemStack item = new ItemStack(potionMaterial);
            if (item.getItemMeta() instanceof PotionMeta potionMeta) {
                potionMeta.setBasePotionType(potionType);
                item.setItemMeta(potionMeta);
            }

            String display = formatPotionDisplayName(potionMaterial, potionType);
            result.add(new OrderableItem(item, potionMaterial, display));
        }
    }

    private static void addEnchantedBookVariants(@NotNull List<OrderableItem> result, @NotNull MaterialBlacklistService blacklist) {
        Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        for (Enchantment enchantment : enchantmentRegistry) {
            String key = enchantment.getKey().getKey();

            for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                if (blacklist.isVariantBlacklisted(Material.ENCHANTED_BOOK, key, level)) continue;

                ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
                if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
                    meta.addStoredEnchant(enchantment, level, true);
                    item.setItemMeta(meta);
                }

                String display = formatEnumName(key) + " " + toRoman(level);
                result.add(new OrderableItem(item, Material.ENCHANTED_BOOK, display));
            }
        }
    }

    private static String formatPotionDisplayName(@NotNull Material potionMaterial, @NotNull PotionType potionType) {
        String prefix = switch (potionMaterial) {
            case SPLASH_POTION -> "Splash Potion of ";
            case LINGERING_POTION -> "Lingering Potion of ";
            case TIPPED_ARROW -> "Arrow of ";
            default -> "Potion of ";
        };

        String name = potionType.name();

        if (name.startsWith("STRONG_")) {
            return prefix + formatEnumName(name.substring("STRONG_".length())) + " II";
        }
        if (name.startsWith("LONG_")) {
            name = name.substring("LONG_".length());
        }

        return prefix + formatEnumName(name);
    }

    public static String formatEnumName(@NotNull String name) {
        String[] words = name.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    public static String toRoman(int number) {
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return number < romans.length ? romans[number] : String.valueOf(number);
    }
}