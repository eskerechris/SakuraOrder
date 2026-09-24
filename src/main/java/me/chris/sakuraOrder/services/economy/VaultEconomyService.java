package me.chris.sakuraOrder.services.economy;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.economy.EconomyService;
import me.chris.sakuraOrder.api.services.economy.result.EconomyResult;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class VaultEconomyService implements EconomyService {

    private final OrderPlugin plugin;
    private Economy economy;

    public VaultEconomyService(OrderPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found: EconomyService disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No Economy providers registered: EconomyService disabled.");
            return;
        }
        this.economy = rsp.getProvider();
    }

    @Override
    public boolean isEnabled() {
        return economy != null;
    }

    @Override
    public BigDecimal getBalance(UUID playerId) {
        if (!isEnabled()) return BigDecimal.ZERO;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return BigDecimal.valueOf(economy.getBalance(player)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean has(UUID playerId, BigDecimal amount) {
        if (!isEnabled()) return false;
        return economy.has(Bukkit.getOfflinePlayer(playerId), amount.doubleValue());
    }

    @Override
    public EconomyResult withdraw(UUID playerId, BigDecimal amount) {
        if (!isEnabled()) {
            return new EconomyResult.Failure(EconomyResult.FailureReason.ECONOMY_UNAVAILABLE, "Economy not available");
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        if (!economy.has(player, amount.doubleValue())) {
            return new EconomyResult.Failure(EconomyResult.FailureReason.INSUFFICIENT_FUNDS, "Insufficient founds");
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount.doubleValue());
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Withdraw failed for " + playerId + ": " + response.errorMessage);
            return new EconomyResult.Failure(EconomyResult.FailureReason.UNKNOWN_ERROR, "Unknown error");
        }
        return new EconomyResult.Success(BigDecimal.valueOf(response.balance));
    }

    @Override
    public EconomyResult deposit(UUID playerId, BigDecimal amount) {
        if (!isEnabled()) {
            return new EconomyResult.Failure(EconomyResult.FailureReason.ECONOMY_UNAVAILABLE, "Economy not available");
        }
        EconomyResponse response = economy.depositPlayer(Bukkit.getOfflinePlayer(playerId), amount.doubleValue());
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Deposit failed for " + playerId + ": " + response.errorMessage);
            return new EconomyResult.Failure(EconomyResult.FailureReason.UNKNOWN_ERROR, "Unknown error");
        }
        return new EconomyResult.Success(BigDecimal.valueOf(response.balance));
    }

    @Override
    public String format(BigDecimal amount) {
        return isEnabled() ? economy.format(amount.doubleValue()) : amount.toPlainString();
    }
}