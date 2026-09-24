package me.chris.sakuraOrder.api.services.order.result;

import me.chris.sakuraOrder.api.model.IOrder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the outcome of an order creation attempt.
 *
 * <p>An order creation attempt either succeeds and returns the newly created
 * order, or fails with a specific reason and a user-facing message.</p>
 */
public sealed interface CreateResult permits CreateResult.Created, CreateResult.Failed {

    /**
     * Checks whether the order creation was successful.
     *
     * @return {@code true} if this result represents a successful creation,
     *         {@code false} otherwise
     */
    default boolean isSuccess() {
        return this instanceof Created;
    }

    /**
     * Represents a successful order creation result.
     *
     * @param order the newly created order
     * @param message the user-facing success message
     */
    record Created(
            @NotNull IOrder order,
            @NotNull Component message
    ) implements CreateResult {}

    /**
     * Represents a failed order creation result.
     *
     * @param reason the specific reason why the creation failed
     * @param message the user-facing failure message
     */
    record Failed(
            @NotNull FailureReason reason,
            @NotNull Component message
    ) implements CreateResult {}

    /**
     * Defines the possible reasons why an order creation attempt may fail.
     */
    enum FailureReason {

        /**
         * The player associated with the creation request is invalid.
         */
        INVALID_PLAYER,

        /**
         * The player does not have sufficient funds to create the order.
         */
        INSUFFICIENT_FUNDS,

        /**
         * The configured economy provider is unavailable.
         */
        ECONOMY_UNAVAILABLE,

        /**
         * The player has reached the maximum number of allowed orders.
         */
        MAX_AMOUNT_REACHED,

        /**
         * The order data failed validation.
         */
        VALIDATION_ERROR,

        /**
         * The order could not be persisted due to a database error.
         */
        DATABASE_ERROR,

        /**
         * An event listener cancelled the order creation attempt.
         */
        CANCELLED_BY_LISTENER,

        /**
         * The order system is currently locked and order creation is disabled.
         */
        LOCKED,

        /**
         * The creation attempt failed for an unspecified reason.
         */
        UNKNOWN
    }
}