package me.chris.sakuraOrder.persistence;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.services.config.SettingsService.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

/**
 * Shared logic between MySQL and MariaDB: same SQL dialect (native TIMESTAMP,
 * LONGBLOB, ON DUPLICATE KEY UPDATE), same connection scheme via remote pool.
 * The only real difference between the two subclasses is the JDBC driver scheme.
 */
public abstract class MySqlLikeDatabase extends Database {

    protected MySqlLikeDatabase(@NotNull SakuraOrder plugin, @NotNull DatabaseSettings settings) {
        super(plugin, settings);
    }

    /** JDBC driver scheme ("mysql" or "mariadb"): the only real difference between the two subclasses. */
    @NotNull
    protected abstract String getJdbcScheme();

    @Override
    protected void configureHikari(@NotNull HikariConfig config) {
        config.setJdbcUrl("jdbc:%s://%s:%d/%s?useSSL=%b".formatted(
                getJdbcScheme(), settings.getHost(), settings.getPort(), settings.getDatabase(), settings.isSsl()));
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setMaximumPoolSize(settings.getMaximumPoolSize());
        config.setMinimumIdle(settings.getMinimumIdle());
        config.setConnectionTimeout(settings.getConnectionTimeoutMs());
        config.setIdleTimeout(settings.getIdleTimeoutMs());
        config.setMaxLifetime(settings.getMaxLifetimeMs());
    }

    @Override
    @NotNull
    public String getCreateActiveOrdersTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS %sorders (
                id VARCHAR(36) NOT NULL PRIMARY KEY,
                buyer_id VARCHAR(36) NOT NULL,
                item_data LONGBLOB NOT NULL,
                total_price DECIMAL(18, 2) NOT NULL,
                price_per_item DECIMAL(18, 2) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP NULL,
                order_status VARCHAR(20) NOT NULL,
                amount BIGINT NOT NULL,
                original_amount BIGINT NOT NULL,
                delivered BIGINT NOT NULL DEFAULT 0,
                collected BIGINT NOT NULL DEFAULT 0,
                INDEX idx_buyer_id (buyer_id),
                INDEX idx_status (order_status),
                INDEX idx_expires_at (expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """.formatted(getTablePrefix());
    }

    @Override
    @NotNull
    public String getCreateHistoryOrdersTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS %sorders_history (
                id VARCHAR(36) NOT NULL PRIMARY KEY,
                buyer_id VARCHAR(36) NOT NULL,
                item_data LONGBLOB NOT NULL,
                total_price DECIMAL(18, 2) NOT NULL,
                price_per_item DECIMAL(18, 2) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP NULL,
                order_status VARCHAR(20) NOT NULL,
                amount BIGINT NOT NULL,
                original_amount BIGINT NOT NULL,
                delivered BIGINT NOT NULL DEFAULT 0,
                collected BIGINT NOT NULL DEFAULT 0,
                archived_at TIMESTAMP NOT NULL,
                INDEX idx_history_buyer_id (buyer_id, archived_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """.formatted(getTablePrefix());
        // No explicit DESC on the index for compatibility with MySQL < 8.0 / MariaDB < 10.8
        // (where it would be ignored). The optimizer can still scan the index backward
        // to satisfy ORDER BY archived_at DESC without a filesort, even in ascending order.
    }

    @Override
    @NotNull
    public List<String> getCreateIndexesSql() {
        // Indexes are already declared inline in the DDL above (MySQL/MariaDB syntax): no additional statements.
        return List.of();
    }

    /**
     * Upsert for an order. {@code amount} is updated because early finalization caps it to
     * {@code delivered}; {@code original_amount} is written only on insert and never changes.
     */
    @Override
    @NotNull
    public String getUpsertOrderSql() {
        return """
            INSERT INTO %sorders (id, buyer_id, item_data, total_price, price_per_item, created_at, expires_at, order_status, amount, delivered, collected, original_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                order_status = VALUES(order_status),
                amount = VALUES(amount),
                delivered = VALUES(delivered),
                collected = VALUES(collected),
                expires_at = VALUES(expires_at);
            """.formatted(getTablePrefix());
    }

    @Override
    public void bindTimestamp(@NotNull PreparedStatement ps, int paramIndex, @Nullable Instant instant) throws SQLException {
        if (instant != null) {
            ps.setTimestamp(paramIndex, Timestamp.from(instant));
        } else {
            ps.setNull(paramIndex, Types.TIMESTAMP);
        }
    }

    @Override
    @Nullable
    public Instant readTimestamp(@NotNull ResultSet rs, @NotNull String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }
}