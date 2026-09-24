package me.chris.sakuraOrder.api.services.economy;

import me.chris.sakuraOrder.api.services.economy.result.EconomyResult;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Provides access to the configured economy system.
 *
 * <p>Implementations are responsible for checking the availability of the
 * economy provider and performing balance operations for players.</p>
 */
public interface EconomyService {

    /**
     * Checks whether an economy provider is currently available.
     *
     * @return {@code true} if the economy system is enabled and available
     */
    boolean isEnabled();

    /**
     * Retrieves the current balance of a player.
     *
     * @param playerId the unique identifier of the player
     * @return the player's current balance
     */
    BigDecimal getBalance(UUID playerId);

    /**
     * Checks whether a player has enough funds to cover the specified amount.
     *
     * @param playerId the unique identifier of the player
     * @param amount the amount to check
     * @return {@code true} if the player has at least the specified amount
     */
    boolean has(UUID playerId, BigDecimal amount);

    /**
     * Withdraws the specified amount from a player's balance.
     *
     * @param playerId the unique identifier of the player
     * @param amount the amount to withdraw
     * @return the result of the withdrawal operation
     */
    EconomyResult withdraw(UUID playerId, BigDecimal amount);

    /**
     * Deposits the specified amount into a player's balance.
     *
     * @param playerId the unique identifier of the player
     * @param amount the amount to deposit
     * @return the result of the deposit operation
     */
    EconomyResult deposit(UUID playerId, BigDecimal amount);

    /**
     * Formats an amount according to the economy provider's configured format.
     *
     * @param amount the amount to format
     * @return the formatted monetary amount
     */
    String format(BigDecimal amount);
}