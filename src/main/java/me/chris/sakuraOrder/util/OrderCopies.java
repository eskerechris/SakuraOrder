package me.chris.sakuraOrder.util;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderStatus;
import me.chris.sakuraOrder.model.Order;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Copy-on-write helpers: each method returns a new order with some fields replaced, leaving the
 * source untouched. Shared by every service that applies a domain transition.
 */
public final class OrderCopies {

    private OrderCopies() {}

    public static IOrder withDelivered(@NotNull IOrder source, long newDelivered) {
        return builderFrom(source).delivered(newDelivered).build();
    }

    public static IOrder withCollected(@NotNull IOrder source, long newCollected) {
        return builderFrom(source).collected(newCollected).build();
    }

    public static IOrder withStatus(@NotNull IOrder source, @NotNull OrderStatus status) {
        return builderFrom(source).orderStatus(status).build();
    }

    public static IOrder withStatusAndExpiry(@NotNull IOrder source, @NotNull OrderStatus status, @NotNull Instant expiresAt) {
        return builderFrom(source).orderStatus(status).expiresAt(expiresAt).build();
    }

    /**
     * Replaces {@code amount} (the effective target) while preserving {@code originalAmount}.
     */
    public static IOrder withAmountAndStatus(@NotNull IOrder source, long newAmount, @NotNull OrderStatus status, @NotNull Instant expiresAt) {
        return builderFrom(source).amount(newAmount).orderStatus(status).expiresAt(expiresAt).build();
    }

    @NotNull
    private static Order.Builder builderFrom(@NotNull IOrder source) {
        if (source instanceof Order concreteOrder) {
            return concreteOrder.toBuilder();
        }
        return Order.builder()
                .id(source.getId())
                .buyerId(source.getBuyerId())
                .itemStack(source.getItemStack())
                .totalPrice(source.getTotalPrice())
                .pricePerItem(source.getPricePerItem())
                .createdAt(source.getCreatedAt())
                .expiresAt(source.getExpiresAt())
                .orderStatus(source.getOrderStatus())
                .amount(source.getAmount())
                .originalAmount(source.getOriginalAmount())
                .delivered(source.getDelivered())
                .collected(source.getCollected());
    }
}