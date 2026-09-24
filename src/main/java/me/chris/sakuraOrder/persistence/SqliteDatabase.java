package me.chris.sakuraOrder.persistence;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.services.config.SettingsService.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

public class SqliteDatabase extends Database {

    public SqliteDatabase(@NotNull SakuraOrder plugin, @NotNull DatabaseSettings settings) {
        super(plugin, settings);
    }

    @Override
    protected void configureHikari(@NotNull HikariConfig config) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create plugin data folder: " + dataFolder);
        }
        File dbFile = new File(dataFolder, settings.getSqliteFileName());
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        // SQLite allows only one writer at a time: pool size set to 1 to avoid
        // "database is locked" errors. WAL journal mode (enabled in initialize())
        // still permits concurrent reads despite the reduced pool size.
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
    }

    @Override
    public void initialize() {
        super.initialize();
        try (Connection conn = getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to enable WAL journal mode on SQLite: " + e.getMessage());
        }
    }

    @Override
    @NotNull
    public String getCreateActiveOrdersTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS %sorders (
                id TEXT PRIMARY KEY NOT NULL,
                buyer_id TEXT NOT NULL,
                item_data BLOB NOT NULL,
                total_price DECIMAL(18, 2) NOT NULL,
                price_per_item DECIMAL(18, 2) NOT NULL,
                created_at INTEGER NOT NULL,
                expires_at INTEGER,
                order_status TEXT NOT NULL,
                amount BIGINT NOT NULL,
                original_amount BIGINT NOT NULL,
                delivered BIGINT NOT NULL DEFAULT 0,
                collected BIGINT NOT NULL DEFAULT 0
            );
            """.formatted(getTablePrefix());
    }

    @Override
    @NotNull
    public String getCreateHistoryOrdersTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS %sorders_history (
                id TEXT PRIMARY KEY NOT NULL,
                buyer_id TEXT NOT NULL,
                item_data BLOB NOT NULL,
                total_price DECIMAL(18, 2) NOT NULL,
                price_per_item DECIMAL(18, 2) NOT NULL,
                created_at INTEGER NOT NULL,
                expires_at INTEGER,
                order_status TEXT NOT NULL,
                amount BIGINT NOT NULL,
                original_amount BIGINT NOT NULL,
                delivered BIGINT NOT NULL DEFAULT 0,
                collected BIGINT NOT NULL DEFAULT 0,
                archived_at INTEGER NOT NULL
            );
            """.formatted(getTablePrefix());
    }

    @Override
    @NotNull
    public List<String> getCreateIndexesSql() {
        String prefix = getTablePrefix();
        return List.of(
                "CREATE INDEX IF NOT EXISTS idx_%1$sorders_buyer ON %1$sorders (buyer_id);".formatted(prefix),
                "CREATE INDEX IF NOT EXISTS idx_%1$sorders_status ON %1$sorders (order_status);".formatted(prefix),
                "CREATE INDEX IF NOT EXISTS idx_%1$sorders_expires ON %1$sorders (expires_at);".formatted(prefix),
                "CREATE INDEX IF NOT EXISTS idx_%1$sorders_history_buyer ON %1$sorders_history (buyer_id, archived_at);".formatted(prefix)
        );
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
            ON CONFLICT(id) DO UPDATE SET
                order_status = excluded.order_status,
                amount = excluded.amount,
                delivered = excluded.delivered,
                collected = excluded.collected,
                expires_at = excluded.expires_at;
            """.formatted(getTablePrefix());
    }

    @Override
    public void bindTimestamp(@NotNull PreparedStatement ps, int paramIndex, @Nullable Instant instant) throws SQLException {
        if (instant != null) {
            ps.setLong(paramIndex, instant.toEpochMilli());
        } else {
            ps.setNull(paramIndex, Types.INTEGER);
        }
    }

    @Override
    @Nullable
    public Instant readTimestamp(@NotNull ResultSet rs, @NotNull String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Instant.ofEpochMilli(value);
    }
}