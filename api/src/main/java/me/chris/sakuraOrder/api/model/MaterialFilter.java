package me.chris.sakuraOrder.api.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Defines the available categories for filtering Minecraft materials.
 *
 * <p>Each filter provides a predicate that determines whether a material
 * belongs to the corresponding category.</p>
 */
public enum MaterialFilter {

    /**
     * Includes all materials.
     */
    ALL(material -> true),

    /**
     * Includes materials that represent blocks.
     */
    BLOCK(MaterialFilter::isBlock),

    /**
     * Includes materials commonly used as tools.
     */
    TOOLS(MaterialFilter::isTool),

    /**
     * Includes edible materials.
     */
    FOOD(MaterialFilter::isFood),

    /**
     * Includes materials commonly used for combat.
     */
    COMBAT(MaterialFilter::isCombat),

    /**
     * Includes potion-related materials.
     */
    POTIONS(MaterialFilter::isPotion),

    /**
     * Includes books and book-related materials.
     */
    BOOKS(MaterialFilter::isBook),

    /**
     * Includes materials that do not belong to any of the other categories.
     */
    MISCELLANEOUS(material -> !isBlock(material) && !isTool(material) && !isFood(material)
            && !isCombat(material) && !isPotion(material) && !isBook(material));

    private final Predicate<Material> predicate;

    MaterialFilter(Predicate<Material> predicate) {
        this.predicate = predicate;
    }

    /**
     * Returns the predicate used to determine whether a material matches this filter.
     *
     * @return the material filter predicate
     */
    @NotNull
    public Predicate<Material> predicate() {
        return predicate;
    }

    /**
     * Determines whether the specified material is a block.
     *
     * @param material the material to check
     * @return {@code true} if the material is a block
     */
    private static boolean isBlock(Material material) {
        return material.isBlock();
    }

    /**
     * Determines whether the specified material is edible.
     *
     * @param material the material to check
     * @return {@code true} if the material is edible
     */
    private static boolean isFood(Material material) {
        return material.isEdible();
    }

    /**
     * Determines whether the specified material is commonly used as a tool.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as a tool
     */
    private static boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.endsWith("_AXE")
                || material == Material.SHEARS || material == Material.FLINT_AND_STEEL
                || material == Material.FISHING_ROD || material == Material.BRUSH
                || material == Material.SPYGLASS || material == Material.COMPASS
                || material == Material.CLOCK || material == Material.LEAD;
    }

    /**
     * Determines whether the specified material is commonly used for combat.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as a combat item
     */
    private static boolean isCombat(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.endsWith("_ARROW")
                || material == Material.BOW || material == Material.CROSSBOW
                || material == Material.TRIDENT || material == Material.SHIELD
                || material == Material.TNT;
    }

    /**
     * Determines whether the specified material is related to potions.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as potion-related
     */
    private static boolean isPotion(Material material) {
        String name = material.name();
        return name.endsWith("_POTION") || material == Material.GLASS_BOTTLE
                || material == Material.BREWING_STAND;
    }

    /**
     * Determines whether the specified material is a book or book-related item.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as a book
     */
    private static boolean isBook(Material material) {
        return material == Material.BOOK || material == Material.WRITABLE_BOOK
                || material == Material.WRITTEN_BOOK || material == Material.ENCHANTED_BOOK
                || material == Material.KNOWLEDGE_BOOK;
    }
}