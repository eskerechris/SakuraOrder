package me.chris.sakuraOrder.api.services.webhook;

import me.chris.sakuraOrder.api.model.WebhookEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Logs marketplace events to an external sink such as Discord.
 */
public interface WebhookService {

    /**
     * Queues an event for asynchronous delivery.
     * <p>
     * This method is safe to call from any thread and must return without
     * waiting for the webhook request to complete.
     *
     * @param event the event to queue
     */
    void send(@NotNull WebhookEvent event);

    /**
     * Reloads the webhook configuration from disk.
     */
    void reload();

    /**
     * Flushes pending messages on a best-effort basis and releases any resources
     * held by the service.
     */
    default void close() {}
}