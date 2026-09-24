package me.chris.sakuraOrder.api.event.create;

import me.chris.sakuraOrder.api.event.OrderEvent;
import me.chris.sakuraOrder.api.model.IOrder;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an order has been successfully validated and persisted into the storage system.
 */
public class OrderCreateEvent extends OrderEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final IOrder order;

    /**
     * Constructs a new {@link OrderCreateEvent}.
     *
     * @param order the created order
     */
    public OrderCreateEvent(@NotNull IOrder order) {
        super(false);
        this.order = order;
    }

    /**
     * Gets the newly created order.
     *
     * @return the created {@link IOrder}
     */
    @NotNull
    public IOrder getOrder() {
        return this.order;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
