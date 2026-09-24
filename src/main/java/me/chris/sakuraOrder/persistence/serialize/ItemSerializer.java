package me.chris.sakuraOrder.persistence.serialize;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemSerializer {

    private ItemSerializer() {}

    /**
     * Serializes the ItemStack into Paper native binary byte array (preserves NBT, components, and PDC).
     */
    public static byte[] toBytes(@NotNull ItemStack item) {
        return item.serializeAsBytes();
    }

    /**
     * Deserializes an ItemStack from the Paper native binary byte array.
     */
    public static @NotNull ItemStack fromBytes(byte[] bytes) {
        return ItemStack.deserializeBytes(bytes);
    }
}