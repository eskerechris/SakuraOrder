package me.chris.sakuraOrder.api.services.config;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.persistence.DatabaseType;
import org.jetbrains.annotations.NotNull;

/**
 * Provides access to SakuraOrder configuration settings.
 *
 * <p>Settings are grouped into general plugin settings and database
 * configuration. Implementations may reload the underlying configuration
 * at runtime through {@link OrderPlugin#reload()}.</p>
 */
public interface SettingsService {

    @NotNull
    GeneralSettings getGeneral();

    @NotNull
    DatabaseSettings getDatabase();

    void reload();

    interface GeneralSettings {
        boolean shouldBroadcast();
        int getOrderExpiration();
        int getClaimGraceDays();
        int getMaxOrderSupported();
        String getOrderMaxItemAmount();

        @NotNull String getLanguage();
        @NotNull String getFallbackLanguage();
    }

    interface DatabaseSettings {
        @NotNull DatabaseType getType();
        @NotNull String getTablePrefix();
        @NotNull String getSqliteFileName();
        @NotNull String getHost();
        int getPort();
        @NotNull String getDatabase();
        @NotNull String getUsername();
        @NotNull String getPassword();
        boolean isSsl();
        int getMaximumPoolSize();
        int getMinimumIdle();
        long getConnectionTimeoutMs();
        long getIdleTimeoutMs();
        long getMaxLifetimeMs();
        long getLeakDetectionThresholdMs();
    }
}
