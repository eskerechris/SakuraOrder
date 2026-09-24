package me.chris.sakuraOrder.services.config;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.persistence.DatabaseType;
import me.chris.sakuraOrder.api.services.config.SettingsService;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class PluginSettingsService implements SettingsService {

    private final OrderPlugin plugin;
    private volatile SettingsHolder currentSettings;

    public PluginSettingsService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        GeneralRecord general = new GeneralRecord(
                config.getBoolean("settings.broadcast-creation", true),
                config.getInt("settings.order-expiration-days", 7),
                config.getInt("settings.claim-grace-days", 90),
                26, // Max 26
                config.getString("settings.order-max-item-amount", "1T"),
                config.getString("settings.language", "en_GB"),
                config.getString("settings.fallback-language", "en_GB")
        );

        DatabaseType type = DatabaseType.fromString(config.getString("database.type", "SQLITE"));
        DatabaseRecord database = new DatabaseRecord(
                type,
                config.getString("database.table-prefix", "sakura_"),
                config.getString("database.sqlite.file-name", "orders.db"),
                config.getString("database.mysql.host", "localhost"),
                config.getInt("database.mysql.port", 3306),
                config.getString("database.mysql.database", "sakura_orders"),
                config.getString("database.mysql.username", "root"),
                config.getString("database.mysql.password", ""),
                config.getBoolean("database.mysql.ssl", false),
                config.getInt("database.pool.maximum-pool-size", 10),
                config.getInt("database.pool.minimum-idle", 2),
                config.getLong("database.pool.connection-timeout-ms", 30000),
                config.getLong("database.pool.idle-timeout-ms", 600000),
                config.getLong("database.pool.max-lifetime-ms", 1800000),
                config.getLong("database.pool.leak-detection-threshold-ms", 10000)
        );

        // Atomic swap of the settings holder to ensure thread safety during reloads
        this.currentSettings = new SettingsHolder(general, database);
    }

    @Override
    public @NotNull GeneralSettings getGeneral() {
        return currentSettings.general();
    }

    @Override
    public @NotNull DatabaseSettings getDatabase() {
        return currentSettings.database();
    }

    // Internal holder class to encapsulate both general and database settings for atomic updates
    private record SettingsHolder(GeneralRecord general, DatabaseRecord database) {
    }

    // Records for structured settings representation
    public record GeneralRecord(
            boolean shouldBroadcast,
            int orderExpiration,
            int claimGraceDays,
            int maxOrderSupported,
            String orderMaxItemAmount,
            @NotNull String language,
            @NotNull String fallbackLanguage
    ) implements GeneralSettings {

        @Override
        public boolean shouldBroadcast() {
            return shouldBroadcast;
        }

        @Override
        public int getOrderExpiration() {
            return orderExpiration;
        }

        @Override
        public int getClaimGraceDays() {
            return claimGraceDays;
        }

        @Override
        public int getMaxOrderSupported() {
            return maxOrderSupported;
        }

        @Override
        public String getOrderMaxItemAmount() {
            return orderMaxItemAmount;
        }

        @Override
        @NotNull
        public String getLanguage() {
            return language;
        }

        @Override
        @NotNull
        public String getFallbackLanguage() {
            return fallbackLanguage;
        }
    }

    public record DatabaseRecord(
            @NotNull DatabaseType type,
            @NotNull String tablePrefix,
            @NotNull String sqliteFileName,
            @NotNull String host,
            int port,
            @NotNull String database,
            @NotNull String username,
            @NotNull String password,
            boolean ssl,
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs,
            long leakDetectionThresholdMs
    ) implements DatabaseSettings {

        @Override
        public @NotNull DatabaseType getType() {
            return type;
        }

        @Override
        public @NotNull String getTablePrefix() {
            return tablePrefix;
        }

        @Override
        public @NotNull String getSqliteFileName() {
            return sqliteFileName;
        }

        @Override
        public @NotNull String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public @NotNull String getDatabase() {
            return database;
        }

        @Override
        public @NotNull String getUsername() {
            return username;
        }

        @Override
        public @NotNull String getPassword() {
            return password;
        }

        @Override
        public boolean isSsl() {
            return ssl;
        }

        @Override
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        @Override
        public int getMinimumIdle() {
            return minimumIdle;
        }

        @Override
        public long getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        @Override
        public long getIdleTimeoutMs() {
            return idleTimeoutMs;
        }

        @Override
        public long getMaxLifetimeMs() {
            return maxLifetimeMs;
        }

        @Override
        public long getLeakDetectionThresholdMs() {
            return leakDetectionThresholdMs;
        }

    }
}
