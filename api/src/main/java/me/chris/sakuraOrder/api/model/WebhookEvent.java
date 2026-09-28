package me.chris.sakuraOrder.api.model;

import me.chris.sakuraOrder.api.services.webhook.WebhookService;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an event that can be logged through a {@link WebhookService}.
 */
public sealed interface WebhookEvent {

    /**
     * Returns the unique identifier of the order associated with this event.
     *
     * @return the order identifier
     */
    @NotNull UUID orderId();

    /**
     * Represents the creation of a new order.
     *
     * @param orderId the unique identifier of the created order
     * @param buyerId the unique identifier of the buyer
     * @param buyerName the name of the buyer
     * @param itemName the name of the ordered item
     * @param amount the amount of items ordered
     * @param pricePerItem the price of each item
     */
    record OrderCreated(
            @NotNull UUID orderId,
            @NotNull UUID buyerId,
            @NotNull String buyerName,
            @NotNull String itemName,
            long amount,
            @NotNull BigDecimal pricePerItem
    ) implements WebhookEvent {}

    /**
     * Represents items being delivered to an order by a seller.
     *
     * @param orderId the unique identifier of the order
     * @param sellerId the unique identifier of the seller
     * @param sellerName the name of the seller
     * @param buyerName the name of the buyer
     * @param itemName the name of the delivered item
     * @param deliveredAmount the amount of items delivered
     * @param earned the amount earned by the seller
     */
    record OrderDelivered(
            @NotNull UUID orderId,
            @NotNull UUID sellerId,
            @NotNull String sellerName,
            @NotNull UUID buyerId,
            @NotNull String buyerName,
            @NotNull String itemName,
            int deliveredAmount,
            @NotNull BigDecimal earned
    ) implements WebhookEvent {}

    /**
     * Represents a buyer collecting items that were delivered to an order.
     *
     * @param orderId the unique identifier of the order
     * @param collectorId the unique identifier of the collector
     * @param collectorName the name of the collector
     * @param itemName the name of the collected item
     * @param collectedAmount the amount of items collected
     */
    record OrderCollected(
            @NotNull UUID orderId,
            @NotNull UUID collectorId,
            @NotNull String collectorName,
            @NotNull String itemName,
            long collectedAmount
    ) implements WebhookEvent {}

    /**
     * Represents an order being cancelled before completion, with the undelivered
     * portion of the order being refunded.
     *
     * @param orderId the unique identifier of the cancelled order
     * @param buyerName the name of the buyer
     * @param cancelledByName the name of the user who cancelled the order
     * @param itemName the name of the ordered item
     * @param amount the total amount of items in the order
     * @param delivered the amount of items already delivered
     * @param refunded the amount refunded to the buyer
     */
    record OrderCancelled(
            @NotNull UUID orderId,
            @NotNull UUID buyerId,
            @NotNull String buyerName,
            @NotNull String cancelledByName,
            @NotNull String itemName,
            long amount,
            long delivered,
            @NotNull BigDecimal refunded
    ) implements WebhookEvent {}
}