package me.chris.sakuraOrder.api.model;

import me.chris.sakuraOrder.api.services.order.OrderCacheService;
import org.jetbrains.annotations.NotNull;

/**
 * Result of an atomic delivery reservation performed by
 * {@link OrderCacheService#reserveDelivery}.
 *
 * @param order the order associated with the allocation
 * @param deliveredAmount the amount of items allocated for delivery
 */
public record DeliveryAllocation(@NotNull IOrder order, int deliveredAmount) {}