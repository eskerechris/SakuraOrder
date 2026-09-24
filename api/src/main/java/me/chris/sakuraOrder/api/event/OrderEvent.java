package me.chris.sakuraOrder.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base abstract class for all SakuraOrder events.
 */
public abstract class OrderEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public OrderEvent() {
        super();
    }

    public OrderEvent(boolean isAsync) {
        super(isAsync);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
