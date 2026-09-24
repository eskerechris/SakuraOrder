package me.chris.sakuraOrder.api.event.create;

import me.chris.sakuraOrder.api.event.CancellableOrderEvent;
import me.chris.sakuraOrder.api.model.IOrder;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired right before an order is persisted/created.
 */
public class OrderPreCreateEvent extends CancellableOrderEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final IOrder order;

    /**
     * Constructs a new {@link OrderPreCreateEvent}.
     *
     * @param order the order pending creation
     */
    public OrderPreCreateEvent(@NotNull IOrder order) {
        super(false);
        this.order = order;
    }

    /**
     * Gets the order that is pending creation.
     *
     * @return the pending {@link IOrder}
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