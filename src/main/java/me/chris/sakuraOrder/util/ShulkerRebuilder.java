package me.chris.sakuraOrder.util;

import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Produces an updated shulker box ItemStack from a new set of contents, without
 * mutating the original ItemStack/meta in place same copy-then-replace pattern
 * already used for shared IOrder state.
 */
public final class ShulkerRebuilder {

    private ShulkerRebuilder() {}

    @NotNull
    public static ItemStack rebuild(@NotNull ItemStack originalShulker, @NotNull List<ItemStack> newContents) {
        ItemStack rebuilt = originalShulker.clone();
        if (!(rebuilt.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return rebuilt;
        }

        shulkerBox.getInventory().setContents(newContents.toArray(new ItemStack[0]));
        shulkerBox.update();

        meta.setBlockState(shulkerBox);
        rebuilt.setItemMeta(meta);
        return rebuilt;
    }
}