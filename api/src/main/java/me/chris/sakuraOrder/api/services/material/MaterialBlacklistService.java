package me.chris.sakuraOrder.api.services.material;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Provides access to the editable material blacklist.
 *
 * <p>The blacklist is loaded from {@code blacklist.yml} and is used to
 * exclude specific vanilla materials and material variants from material
 * selection interfaces when creating orders.</p>
 */
public interface MaterialBlacklistService {

    /**
     * Reloads the material blacklist from disk.
     *
     * <p>If the blacklist file does not exist, a default file is created.
     * Unknown or invalid material names are skipped and logged as warnings.</p>
     */
    void reload();

    /**
     * Checks whether the specified material is currently blacklisted.
     *
     * @param material the material to check
     * @return {@code true} if the material is blacklisted
     */
    boolean isBlacklisted(@NotNull Material material);

    /**
     * Checks whether a specific material variant is blacklisted.
     *
     * <p>A variant is considered blacklisted if either the base material is
     * blacklisted or a variant-specific rule matches the supplied values.
     * Variant rules use the {@code MATERIAL:VARIANT[:LEVEL]} format in
     * {@code blacklist.yml}.</p>
     *
     * @param material the base material, such as {@link Material#POTION} or
     *                 {@link Material#ENCHANTED_BOOK}
     * @param variantId the variant identifier, such as a potion type name or
     *                  enchantment key; matching is case-insensitive
     * @param level the enchantment level, or {@code null} when not applicable
     *              to the variant
     * @return {@code true} if the material or specified variant is blacklisted
     */
    boolean isVariantBlacklisted(
            @NotNull Material material,
            @NotNull String variantId,
            @Nullable Integer level
    );

    /**
     * Returns an unmodifiable snapshot of the currently blacklisted materials.
     *
     * @return an unmodifiable set containing the currently blacklisted materials
     */
    @NotNull
    Set<Material> getBlacklisted();
}