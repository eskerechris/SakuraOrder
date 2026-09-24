package me.chris.sakuraOrder.api.services.economy.result;

import java.math.BigDecimal;

/**
 * Represents the result of an economy operation.
 *
 * <p>An operation either completes successfully and provides the resulting
 * balance, or fails with a specific reason and descriptive message.</p>
 */
public sealed interface EconomyResult permits EconomyResult.Success, EconomyResult.Failure {

    /**
     * Represents a successful economy operation.
     *
     * @param newBalance the player's balance after the operation
     */
    record Success(BigDecimal newBalance) implements EconomyResult {}

    /**
     * Represents a failed economy operation.
     *
     * @param reason the specific {@link FailureReason} describing why the operation failed
     * @param message a descriptive explanation of the failure
     */
    record Failure(FailureReason reason, String message) implements EconomyResult {}

    /**
     * Defines the possible reasons why an economy operation may fail.
     */
    enum FailureReason {

        /**
         * The player does not have enough funds to complete the operation.
         */
        INSUFFICIENT_FUNDS,

        /**
         * The configured economy provider is unavailable.
         */
        ECONOMY_UNAVAILABLE,

        /**
         * The operation failed for an unspecified or unexpected reason.
         */
        UNKNOWN_ERROR
    }
}