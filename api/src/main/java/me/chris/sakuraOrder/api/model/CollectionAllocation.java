package me.chris.sakuraOrder.api.model;

import me.chris.sakuraOrder.api.services.order.OrderCacheService;
import org.jetbrains.annotations.NotNull;

/**
 * Result of an atomic collection reservation performed by
 * {@link OrderCacheService#reserveCollection}.
 *
 * @param order          the order after the reservation, reflecting the updated collected amount
 *                       (and possibly a transition to {@link OrderStatus#COMPLETED})
 * @param collectedAmount the amount actually allocated to the collector; may be less than requested
 *                        (capped by what is collectable), or zero if nothing was available
 */
public record CollectionAllocation(@NotNull IOrder order, int collectedAmount) {}