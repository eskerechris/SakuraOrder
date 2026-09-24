package me.chris.sakuraOrder.api.event;

import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Base abstract class for {@link OrderEvent} events that can be canceled by listeners.
 */
public abstract class CancellableOrderEvent extends OrderEvent implements Cancellable {

    private boolean cancelled;
    private String reason = "Event was cancelled by a listener";

    public CancellableOrderEvent() {
        super();
    }

    public CancellableOrderEvent(boolean isAsync) {
        super(isAsync);
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Gets the cancellation reason provided by the cancelling listener or system.
     *
     * @return the cancellation reason
     */
    @NotNull
    public String getReason() {
        return this.reason;
    }

    /**
     * Sets the cancellation reason explaining why this event was cancelled.
     *
     * @param reason the descriptive cancellation reason
     */
    public void setReason(@NotNull String reason) {
        this.reason = reason;
    }
}
