package me.chris.sakuraOrder.api.model;

/**
 * Represents the lifecycle statuses of an order.
 */
public enum OrderStatus {

    /**
     * The order is open and actively awaiting fulfillment from other players.
     */
    ACTIVE,

    /**
     * All items have been delivered (delivered == amount).
     * The order no longer accepts deliveries and is not shown on the public marketplace.
     * It is waiting for the buyer to claim items or money within the claim grace period.
     */
    FULFILLED,

    /**
     * The order has been finalized: the buyer has claimed everything,
     * OR the claim grace period expired without claim.
     * From this point on, the order is eligible for archiving into orders_history.
     */
    COMPLETED,

    /**
     * The order was manually canceled by the buyer or an administrator.
     */
    CANCELLED,

    /**
     * The order reached its configured expiration date without being fully completed.
     */
    EXPIRED
}