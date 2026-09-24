package me.chris.sakuraOrder.api.services.order;

/**
 * Periodically expires overdue orders and refunds undelivered items.
 */
public interface OrderExpirationService {
    void start();
    void stop();
}