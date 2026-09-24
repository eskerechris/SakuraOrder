package me.chris.sakuraOrder.persistence;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.services.config.SettingsService.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * Base database implementation shared by all supported SQL dialects.
 */
public abstract class Database {

    protected final SakuraOrder plugin;
    protected final DatabaseSettings settings;
    protected HikariDataSource dataSource;

    protected Database(@NotNull SakuraOrder plugin, @NotNull DatabaseSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /**
     * Creates the database implementation for the configured dialect.
     *
     * @param plugin the plugin instance
     * @param settings the database configuration
     * @return the database implementation for the configured dialect
     */
    @NotNull
    public static Database create(@NotNull SakuraOrder plugin, @NotNull DatabaseSettings settings) {
        return switch (settings.getType()) {
            case SQLITE -> new SqliteDatabase(plugin, settings);
            case MYSQL -> new MySqlDatabase(plugin, settings);
            case MARIADB -> new MariaDbDatabase(plugin, settings);
        };
    }

    /**
     * Initializes the connection pool and creates the database schema.
     */
    public void initialize() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("SakuraOrder-" + getClass().getSimpleName());
        config.setLeakDetectionThreshold(settings.getLeakDetectionThresholdMs());
        configureHikari(config);
        this.dataSource = new HikariDataSource(config);
        createSchema();
    }

    protected abstract void configureHikari(@NotNull HikariConfig config);

    @NotNull
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized: call initialize() before use");
        }
        return dataSource.getConnection();
    }

    private void createSchema() {
        try (Connection conn = getConnection(); var stmt = conn.createStatement()) {
            stmt.execute(getCreateActiveOrdersTableSql());
            stmt.execute(getCreateHistoryOrdersTableSql());
            for (String indexSql : getCreateIndexesSql()) {
                stmt.execute(indexSql);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database schema: " + e.getMessage());
            throw new IllegalStateException("Unable to initialize database schema", e);
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @NotNull
    public String getTablePrefix() {
        return settings.getTablePrefix();
    }

    @NotNull
    public String getPaginationClause(int limit, int offset) {
        return "LIMIT " + limit + " OFFSET " + offset;
    }

    @NotNull
    public abstract String getCreateActiveOrdersTableSql();

    @NotNull
    public abstract String getCreateHistoryOrdersTableSql();

    @NotNull
    public abstract List<String> getCreateIndexesSql();

    @NotNull
    public abstract String getUpsertOrderSql();

    public abstract void bindTimestamp(
            @NotNull PreparedStatement ps,
            int paramIndex,
            @Nullable Instant instant
    ) throws SQLException;

    @Nullable
    public abstract Instant readTimestamp(
            @NotNull ResultSet rs,
            @NotNull String column
    ) throws SQLException;
}