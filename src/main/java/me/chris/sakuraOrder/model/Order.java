package me.chris.sakuraOrder.model;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderStatus;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete implementation of the {@link IOrder} domain entity.
 */
public class Order implements IOrder {

    private final UUID id;
    private final UUID buyerId;
    private final ItemStack itemStack;
    private final BigDecimal totalPrice;
    private final BigDecimal pricePerItem;
    private final Instant createdAt;
    private @Nullable Instant expiresAt;
    private OrderStatus orderStatus;
    private final long amount;
    private final long originalAmount;
    private long delivered;
    private long collected;

    public Order(
            @NotNull UUID id,
            @NotNull UUID buyerId,
            @NotNull ItemStack itemStack,
            @NotNull BigDecimal totalPrice,
            @NotNull BigDecimal pricePerItem,
            @NotNull Instant createdAt,
            @Nullable Instant expiresAt,
            @NotNull OrderStatus orderStatus,
            long amount,
            long originalAmount,
            long delivered,
            long collected
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.buyerId = Objects.requireNonNull(buyerId, "buyerId cannot be null");
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack cannot be null").clone();
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice cannot be null");
        this.pricePerItem = Objects.requireNonNull(pricePerItem, "pricePerItem cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.expiresAt = expiresAt;
        this.orderStatus = Objects.requireNonNull(orderStatus, "orderStatus cannot be null");
        this.amount = amount;
        this.originalAmount = originalAmount > 0 ? originalAmount : amount;
        this.delivered = delivered;
        this.collected = collected;
    }

    @Override
    @NotNull
    public UUID getId() {
        return id;
    }

    @Override
    @NotNull
    public UUID getBuyerId() {
        return buyerId;
    }

    @Override
    @NotNull
    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    @Override
    @NotNull
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    @Override
    @NotNull
    public BigDecimal getPricePerItem() {
        return pricePerItem;
    }

    @Override
    @NotNull
    public BigDecimal getAmountPaid() {
        return BigDecimal.valueOf(delivered).multiply(pricePerItem);
    }

    @Override
    @NotNull
    public BigDecimal getAvailableMoney() {
        return BigDecimal.valueOf(getRemainingAmount()).multiply(pricePerItem);
    }

    @Override
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    @Nullable
    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public void setExpiresAt(@Nullable Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    @NotNull
    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    @Override
    public void setOrderStatus(@NotNull OrderStatus orderStatus) {
        this.orderStatus = Objects.requireNonNull(orderStatus, "orderStatus cannot be null");
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getOriginalAmount() {
        return originalAmount;
    }

    @Override
    public long getDelivered() {
        return delivered;
    }

    @Override
    public void setDelivered(long delivered) {
        this.delivered = delivered;
    }

    @Override
    public long getCollected() {
        return collected;
    }

    @Override
    public void setCollected(long collected) {
        this.collected = collected;
    }

    @Override
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .buyerId(this.buyerId)
                .itemStack(this.itemStack)
                .totalPrice(this.totalPrice)
                .pricePerItem(this.pricePerItem)
                .createdAt(this.createdAt)
                .expiresAt(this.expiresAt)
                .orderStatus(this.orderStatus)
                .amount(this.amount)
                .originalAmount(this.originalAmount)
                .delivered(this.delivered)
                .collected(this.collected);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private UUID buyerId;
        private ItemStack itemStack;
        private BigDecimal totalPrice;
        private BigDecimal pricePerItem;
        private Instant createdAt = Instant.now();
        private Instant expiresAt;
        private OrderStatus orderStatus = OrderStatus.ACTIVE;
        private long amount = 1;
        private long originalAmount = 0;
        private long delivered = 0;
        private long collected = 0;

        public Builder id(@NotNull UUID id) {
            this.id = id;
            return this;
        }

        public Builder buyerId(@NotNull UUID buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public Builder itemStack(@NotNull ItemStack itemStack) {
            this.itemStack = itemStack.clone();
            return this;
        }

        public Builder totalPrice(@NotNull BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public Builder pricePerItem(@NotNull BigDecimal pricePerItem) {
            this.pricePerItem = pricePerItem;
            return this;
        }

        public Builder createdAt(@NotNull Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(@Nullable Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder orderStatus(@NotNull OrderStatus orderStatus) {
            this.orderStatus = orderStatus;
            return this;
        }

        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder originalAmount(long originalAmount) {
            this.originalAmount = originalAmount;
            return this;
        }

        public Builder delivered(long delivered) {
            this.delivered = delivered;
            return this;
        }

        public Builder collected(long collected) {
            this.collected = collected;
            return this;
        }

        @NotNull
        public Order build() {
            Objects.requireNonNull(buyerId, "buyerId is required");
            Objects.requireNonNull(itemStack, "itemStack is required");

            if (pricePerItem == null && totalPrice != null && amount > 0) {
                pricePerItem = totalPrice.divide(BigDecimal.valueOf(amount), 2, RoundingMode.HALF_UP);
            }
            if (totalPrice == null && pricePerItem != null) {
                totalPrice = pricePerItem.multiply(BigDecimal.valueOf(amount));
            }

            return new Order(
                    id, buyerId, itemStack, totalPrice,
                    pricePerItem, createdAt, expiresAt,
                    orderStatus, amount, originalAmount,
                    delivered, collected
            );
        }
    }
}