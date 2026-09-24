package me.chris.sakuraOrder.util;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides read-only helpers for inspecting shulker box item stacks.
 */
public final class ShulkerUtil {

    private ShulkerUtil() {}

    public static boolean isShulkerBox(@NotNull Material material) {
        return material.name().endsWith("SHULKER_BOX");
    }

    public record Extraction(
            @NotNull List<ItemStack> extracted,
            @NotNull ItemStack rebuiltShulker
    ) {}

    /**
     * Extracts up to {@code maxAmount} of items matching {@code reference} from
     * the shulker's inventory, preserving all remaining contents in a rebuilt
     * shulker.
     *
     * <p>Items are processed in inventory order and the final matching stack is
     * split when necessary. If no matching items can be extracted, the original
     * shulker is returned unchanged.</p>
     */
    @NotNull
    public static Extraction extract(
            @NotNull ItemStack shulkerStack,
            @NotNull ItemStack reference,
            int maxAmount
    ) {
        if (!(shulkerStack.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return new Extraction(List.of(), shulkerStack);
        }

        List<ItemStack> extracted = new ArrayList<>();
        List<ItemStack> remainingContents = new ArrayList<>();
        int remaining = maxAmount;

        for (ItemStack slotItem : shulkerBox.getInventory().getContents()) {
            if (slotItem == null || slotItem.getType().isAir()) continue;

            if (remaining <= 0 || !reference.isSimilar(slotItem)) {
                remainingContents.add(slotItem);
                continue;
            }

            int amount = slotItem.getAmount();
            if (amount <= remaining) {
                extracted.add(slotItem);
                remaining -= amount;
            } else {
                ItemStack takenPart = slotItem.clone();
                takenPart.setAmount(remaining);
                extracted.add(takenPart);

                ItemStack leftPart = slotItem.clone();
                leftPart.setAmount(amount - remaining);
                remainingContents.add(leftPart);

                remaining = 0;
            }
        }

        if (extracted.isEmpty()) {
            return new Extraction(List.of(), shulkerStack);
        }

        return new Extraction(
                extracted,
                ShulkerRebuilder.rebuild(shulkerStack, remainingContents)
        );
    }
}