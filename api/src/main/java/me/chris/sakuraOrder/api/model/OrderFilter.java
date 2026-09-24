package me.chris.sakuraOrder.api.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * Defines the available category filters for orders.
 *
 * <p>Each filter provides a predicate that determines whether an order's
 * item belongs to the corresponding category. Filters can be applied before
 * sorting and pagination when querying orders.</p>
 *
 * <p>Material categories are determined using Bukkit's material properties
 * and predefined material classifications.</p>
 */
public enum OrderFilter {

    /**
     * Includes all orders.
     */
    ALL(order -> true),

    /**
     * Includes orders containing block materials.
     */
    BLOCK(order -> materialOf(order).isBlock()),

    /**
     * Includes orders containing materials commonly used as tools.
     */
    TOOLS(order -> isTool(materialOf(order))),

    /**
     * Includes orders containing edible materials.
     */
    FOOD(order -> materialOf(order).isEdible()),

    /**
     * Includes orders containing materials commonly used for combat.
     */
    COMBAT(order -> isCombat(materialOf(order))),

    /**
     * Includes orders containing potion-related materials.
     */
    POTIONS(order -> isPotion(materialOf(order))),

    /**
     * Includes orders containing books and book-related materials.
     */
    BOOKS(order -> isBook(materialOf(order))),

    /**
     * Includes orders whose materials do not belong to any of the other categories.
     */
    MISCELLANEOUS(order -> isMiscellaneous(materialOf(order)));

    private final Predicate<IOrder> predicate;

    OrderFilter(Predicate<IOrder> predicate) {
        this.predicate = predicate;
    }

    /**
     * Returns the predicate used to determine whether an order matches this filter.
     *
     * @return the order filter predicate
     */
    public Predicate<IOrder> predicate() {
        return predicate;
    }

    /**
     * Returns the material associated with an order.
     *
     * @param order the order whose material should be retrieved
     * @return the material contained in the order's item stack
     */
    private static Material materialOf(IOrder order) {
        ItemStack stack = order.getItemStack();
        return stack.getType();
    }

    /**
     * Determines whether the specified material is commonly used as a tool.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as a tool
     */
    private static boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || material == Material.SHEARS
                || material == Material.FISHING_ROD
                || material == Material.FLINT_AND_STEEL
                || material == Material.BRUSH
                || material == Material.COMPASS
                || material == Material.CLOCK
                || material == Material.SPYGLASS
                || material == Material.LEAD;
    }

    /**
     * Determines whether the specified material is commonly used for combat.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as a combat item
     */
    private static boolean isCombat(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || material == Material.BOW
                || material == Material.CROSSBOW
                || material == Material.TRIDENT
                || material == Material.MACE
                || material == Material.SHIELD
                || material == Material.ARROW
                || material == Material.SPECTRAL_ARROW
                || material == Material.TNT
                || material == Material.TURTLE_HELMET
                || material == Material.ELYTRA
                || material == Material.END_CRYSTAL;
    }

    /**
     * Determines whether the specified material is related to potions.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as potion-related
     */
    private static boolean isPotion(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION
                || material == Material.TIPPED_ARROW
                || material == Material.GLASS_BOTTLE
                || material == Material.EXPERIENCE_BOTTLE
                || material == Material.BREWING_STAND
                || material == Material.CAULDRON;
    }

    /**
     * Determines whether the specified material is a book or book-related item.
     *
     * @param material the material to check
     * @return {@code true} if the material is classified as a book
     */
    private static boolean isBook(Material material) {
        return material == Material.BOOK
                || material == Material.WRITABLE_BOOK
                || material == Material.WRITTEN_BOOK
                || material == Material.ENCHANTED_BOOK
                || material == Material.KNOWLEDGE_BOOK;
    }

    /**
     * Determines whether the specified material does not belong to any
     * of the other specific categories.
     *
     * @param material the material to check
     * @return {@code true} if the material is miscellaneous
     */
    private static boolean isMiscellaneous(Material material) {
        return !material.isBlock()
                && !isTool(material)
                && !material.isEdible()
                && !isCombat(material)
                && !isPotion(material)
                && !isBook(material);
    }
}