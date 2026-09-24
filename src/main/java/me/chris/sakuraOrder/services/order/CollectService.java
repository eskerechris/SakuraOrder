package me.chris.sakuraOrder.services.order;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.CollectionAllocation;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.api.services.order.OrderCollectService;
import me.chris.sakuraOrder.api.services.order.result.CollectResult;
import me.chris.sakuraOrder.api.services.order.result.CollectResult.FailureReason;
import me.chris.sakuraOrder.util.NumberParser;
import me.chris.sakuraOrder.util.OrderMaintenanceLock;
import me.chris.sakuraOrder.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CollectService implements OrderCollectService {

    private final OrderPlugin plugin;
    private final LangService lang;

    public CollectService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLangService();
    }

    @Override
    @NotNull
    public CompletableFuture<CollectResult> collect(@NotNull UUID orderId, @NotNull Player collector,
                                                    int requestedAmount, @NotNull CollectMode mode) {
        if (OrderMaintenanceLock.isLocked()) {
            return CompletableFuture.completedFuture(
                    new CollectResult.Failed(FailureReason.LOCKED, lang.message("order.locked"))
            );
        }

        if (requestedAmount <= 0) {
            return CompletableFuture.completedFuture(
                    new CollectResult.Failed(FailureReason.INVALID_REQUEST, lang.message("order.invalid-collect-amount"))
            );
        }

        // Snapshot used only for the ownership check; the collectable amount is
        // re-validated atomically by reserveCollection().
        IOrder snapshot = plugin.getOrderCacheService().get(orderId);
        if (snapshot == null) {
            return CompletableFuture.completedFuture(
                    new CollectResult.Failed(FailureReason.ORDER_NOT_FOUND, lang.message("order.not-found"))
            );
        }

        if (!snapshot.getBuyerId().equals(collector.getUniqueId())) {
            return CompletableFuture.completedFuture(
                    new CollectResult.Failed(FailureReason.NOT_BUYER, lang.message("order.not-your-order"))
            );
        }

        // The collectable amount is determined atomically by reserveCollection(),
        // including the state update, persistence, and possible COMPLETED transition.
        CollectionAllocation allocation = plugin.getOrderCacheService().reserveCollection(orderId, requestedAmount);
        if (allocation == null) {
            return CompletableFuture.completedFuture(
                    new CollectResult.Failed(FailureReason.ORDER_NOT_FOUND, lang.message("order.not-found"))
            );
        }

        int collectedNow = allocation.collectedAmount();
        if (collectedNow == 0) {
            return CompletableFuture.completedFuture(
                    new CollectResult.Failed(FailureReason.NOTHING_TO_COLLECT, lang.message("order.nothing-to-collect"))
            );
        }

        IOrder updatedOrder = allocation.order();

        List<ItemStack> stacks = splitIntoStacks(updatedOrder.getItemStack().getType(), collectedNow);
        giveOrDrop(collector, stacks, mode);

        Map<String, String> placeholders = Map.of(
                "amount", NumberParser.formatNumber(collectedNow),
                "item_name", StringUtil.formatMaterial(updatedOrder.getItemStack())
        );

        return CompletableFuture.completedFuture(
                new CollectResult.Collected(updatedOrder, collectedNow, lang.message("order.collected", placeholders))
        );
    }

    /**
     * Applies the player-facing collection effects synchronously on the collector's
     * owning thread.
     *
     * <p>{@link CollectMode#TO_INVENTORY} adds items to the inventory and drops any
     * overflow, while {@link CollectMode#DROP_AT_FEET} drops all collected items
     * in front of the player's location.</p>
     */
    private void giveOrDrop(@NotNull Player player, @NotNull List<ItemStack> stacks, @NotNull CollectMode mode) {
        SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> {
            if (mode == CollectMode.TO_INVENTORY) {
                var leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
                leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            } else {
                tossInFrontOf(player, stacks);
            }
        });
    }

    /**
     * Drops an item in front of the player with a slight forward and upward velocity
     * to mimic the motion of an item dropped with the Q key.
     */
    private void tossInFrontOf(@NotNull Player player, @NotNull List<ItemStack> stacks) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Location dropLocation = player.getLocation().clone()
                .add(0, 1.0, 0)
                .add(direction.clone().multiply(0.25));

        for (ItemStack stack : stacks) {
            Item entity = player.getWorld().dropItem(dropLocation, stack);

            Vector velocity = direction.clone().multiply(0.15);
            velocity.setY(0.12);

            entity.setVelocity(velocity);
        }
    }

    private List<ItemStack> splitIntoStacks(@NotNull Material material, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        int maxStack = material.getMaxStackSize();
        while (remaining > 0) {
            int chunk = Math.min(remaining, maxStack);
            stacks.add(new ItemStack(material, chunk));
            remaining -= chunk;
        }
        return stacks;
    }
}