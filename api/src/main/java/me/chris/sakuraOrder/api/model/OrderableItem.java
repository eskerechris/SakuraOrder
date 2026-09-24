package me.chris.sakuraOrder.api.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A single selectable entry in the Choose Material menu: a pre-configured
 * ItemStack (possibly with meta data, e.g., a specific potion effect or a
 * spell + level), along with the base Material (used for
 * categorizing the filter) and a readable name (used for searching,
 * sorting, and lore).
 */
public record OrderableItem(@NotNull ItemStack itemStack, @NotNull Material baseMaterial, @NotNull String displayName) {}