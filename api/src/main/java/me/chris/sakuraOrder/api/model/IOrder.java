package me.chris.sakuraOrder.api.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an Order contract within the SakuraOrder plugin.
 */
public interface IOrder {

    /**
     * Gets the unique identifier of the order.
     *
     * @return the order {@link UUID}
     */
    @NotNull
    UUID getId();

    /**
     * Gets the UUID of the player who created/placed the order.
     *
     * @return buyer {@link UUID}
     */
    @NotNull
    UUID getBuyerId();

    /**
     * Gets the ItemStack template requested in this order.
     *
     * @return a cloned copy of the requested {@link ItemStack}
     */
    @NotNull
    ItemStack getItemStack();

    /**
     * Gets the total price offered for the complete order amount.
     *
     * @return total price as {@link BigDecimal}
     */
    @NotNull
    BigDecimal getTotalPrice();

    /**
     * Gets the unit price offered per single item.
     *
     * @return unit price as {@link BigDecimal}
     */
    @NotNull
    BigDecimal getPricePerItem();

    /**
     * Gets the total amount of money given to sellers so far.
     * Calculated dynamically as delivered * pricePerItem.
     *
     * @return amount paid as {@link BigDecimal}
     */
    @NotNull
    default BigDecimal getAmountPaid() {
        return BigDecimal.valueOf(getDelivered()).multiply(getPricePerItem());
    }

    /**
     * Gets the remaining money available for sellers fulfilling the order.
     * Calculated dynamically as remainingAmount * pricePerItem.
     *
     * @return available money as {@link BigDecimal}
     */
    @NotNull
    default BigDecimal getAvailableMoney() {
        return BigDecimal.valueOf(getRemainingAmount()).multiply(getPricePerItem());
    }

    /**
     * Gets the creation timestamp of the order.
     *
     * @return creation {@link Instant}
     */
    @NotNull
    Instant getCreatedAt();

    /**
     * Gets the expiration timestamp of the order, if an expiration was configured.
     * <p>
     * For {@link OrderStatus#ACTIVE} orders, this represents the marketplace listing
     * expiration deadline. For {@link OrderStatus#FULFILLED} orders, this represents
     * the claim grace period deadline for the buyer to collect items.
     *
     * @return expiration {@link Instant}, or {@code null} if it never expires
     */
    @Nullable
    Instant getExpiresAt();

    /**
     * Sets or updates the expiration timestamp of the order.
     *
     * @param expiresAt the new expiration {@link Instant}, or {@code null}
     */
    void setExpiresAt(@Nullable Instant expiresAt);

    /**
     * Gets the current lifecycle status of the order.
     *
     * @return the {@link OrderStatus}
     */
    @NotNull
    OrderStatus getOrderStatus();

    /**
     * Updates the status of the order.
     *
     * @param status the new {@link OrderStatus}
     */
    void setOrderStatus(@NotNull OrderStatus status);

    /**
     * Gets the total quantity of items requested.
     *
     * @return requested total amount
     */
    long getAmount();

    /**
     * Gets the quantity originally requested, unaffected by early finalization.
     *
     * @return original requested amount
     */
    default long getOriginalAmount() {
        return getAmount();
    }

    /**
     * Gets the number of items already delivered/fulfilled.
     *
     * @return delivered item count
     */
    long getDelivered();

    /**
     * Updates the number of items delivered.
     *
     * @param delivered new delivered count
     */
    void setDelivered(long delivered);

    /**
     * Gets the number of items already collected/claimed by the buyer.
     *
     * @return collected item count
     */
    long getCollected();

    /**
     * Updates the number of items collected by the buyer.
     *
     * @param collected new collected count
     */
    void setCollected(long collected);

    /**
     * Gets the remaining amount of items needed to fulfill the order.
     *
     * @return remaining count to deliver
     */
    default long getRemainingAmount() {
        return Math.max(0, getAmount() - getDelivered());
    }

    /**
     * Gets the amount of delivered items the buyer has not yet collected.
     *
     * @return remaining count to collect
     */
    default long getCollectableAmount() {
        return Math.max(0, getDelivered() - getCollected());
    }

    /**
     * Checks if there is any delivered item the buyer has not yet collected.
     *
     * @return {@code true} if {@link #getCollectableAmount()} is greater than zero
     */
    default boolean canCollect() {
        return getCollectableAmount() > 0;
    }

    /**
     * Checks if the buyer has collected the full requested amount.
     *
     * @return {@code true} if collected count is greater than or equal to the total amount
     */
    default boolean isFullyCollected() {
        return getCollected() >= getAmount();
    }

    /**
     * Checks if all requested items have been delivered (delivered >= amount), or if status is FULFILLED.
     *
     * @return {@code true} if delivered count is greater than or equal to total amount, or status is FULFILLED
     */
    default boolean isFulfilled() {
        return getOrderStatus() == OrderStatus.FULFILLED || getDelivered() >= getAmount();
    }

    /**
     * Checks if the order is finalized/completed (claimed by buyer or grace period expired).
     *
     * @return {@code true} if order status is COMPLETED
     */
    default boolean isCompleted() {
        return getOrderStatus() == OrderStatus.COMPLETED;
    }

    /**
     * Checks if the order has passed its expiration instant.
     *
     * @return {@code true} if expired, {@code false} otherwise
     */
    boolean isExpired();

    /**
     * Checks if the order is active and ready to accept deliveries.
     *
     * @return {@code true} if status is ACTIVE, not expired, and not fulfilled
     */
    default boolean isActive() {
        return getOrderStatus() == OrderStatus.ACTIVE && !isExpired() && !isFulfilled() && !isCompleted();
    }
}