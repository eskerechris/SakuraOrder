package me.chris.sakuraOrder.api.services.order;

import me.chris.sakuraOrder.api.services.order.result.DeliveryResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides operations for validating and processing item deliveries toward orders.
 */
public interface OrderDeliveryService {

    /**
     * Attempts to deliver the provided items toward an order's remaining amount.
     *
     * <p>The order is retrieved from the cache using its identifier before the
     * delivery is calculated, ensuring that the operation does not rely on a
     * stale order snapshot held by the caller.</p>
     *
     * <p>If the provided items exceed the order's remaining amount, only the
     * required amount is delivered. Any excess is returned to the deliverer's
     * inventory, with items that cannot fit being dropped at the deliverer's
     * location.</p>
     *
     * @param orderId the unique identifier of the order to deliver to
     * @param providedStacks the item stacks provided for delivery
     * @param deliverer the player performing the delivery and receiving any
     *                  undeliverable excess
     * @return a future completed with the result of the delivery attempt
     */
    @NotNull
    CompletableFuture<DeliveryResult> deliver(
            @NotNull UUID orderId,
            @NotNull List<ItemStack> providedStacks,
            @NotNull Player deliverer
    );
}