package me.chris.sakuraOrder.persistence;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderStatus;
import me.chris.sakuraOrder.api.persistence.OrderRepository;
import me.chris.sakuraOrder.model.Order;
import me.chris.sakuraOrder.persistence.serialize.ItemSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * SQL implementation of {@link OrderRepository}.
 */
public class SqlOrderRepository implements OrderRepository {

    private final SakuraOrder plugin;
    private final Database database;

    public SqlOrderRepository(@NotNull SakuraOrder plugin, @NotNull Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> save(@NotNull IOrder order) {
        return CompletableFuture.runAsync(() -> {
            String sql = database.getUpsertOrderSql();
            try (Connection conn = database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, order.getId().toString());
                ps.setString(2, order.getBuyerId().toString());
                ps.setBytes(3, ItemSerializer.toBytes(order.getItemStack()));
                ps.setBigDecimal(4, order.getTotalPrice());
                ps.setBigDecimal(5, order.getPricePerItem());
                database.bindTimestamp(ps, 6, order.getCreatedAt());
                database.bindTimestamp(ps, 7, order.getExpiresAt());
                ps.setString(8, order.getOrderStatus().name());
                ps.setLong(9, order.getAmount());
                ps.setLong(10, order.getDelivered());
                ps.setLong(11, order.getCollected());
                ps.setLong(12, order.getOriginalAmount());

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while saving order " + order.getId(), e);
                throw new RuntimeException("Database operation failed while saving order " + order.getId(), e);
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<IOrder>> findById(@NotNull UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + database.getTablePrefix() + "orders WHERE id = ?";
            try (Connection conn = database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while searching for order " + id, e);
                throw new RuntimeException("Failed to search for order " + id, e);
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> deleteById(@NotNull UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM " + database.getTablePrefix() + "orders WHERE id = ?";
            try (Connection conn = database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while deleting order " + id, e);
                throw new RuntimeException("Failed to delete order " + id, e);
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<List<IOrder>> findActiveOrders() {
        return CompletableFuture.supplyAsync(() -> {
            List<IOrder> list = new ArrayList<>();
            String sql = "SELECT * FROM " + database.getTablePrefix() + "orders WHERE order_status IN ('ACTIVE', 'FULFILLED')";
            try (Connection conn = database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while loading active orders", e);
                throw new RuntimeException("Failed to load active orders", e);
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<List<IOrder>> findByBuyerId(@NotNull UUID buyerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<IOrder> list = new ArrayList<>();
            String sql = "SELECT * FROM " + database.getTablePrefix() + "orders WHERE buyer_id = ?";
            try (Connection conn = database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, buyerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while loading orders for buyer " + buyerId, e);
                throw new RuntimeException("Failed to load orders for buyer " + buyerId, e);
            }
        });
    }

    @Override
    @NotNull
    public CompletableFuture<List<IOrder>> findOrderHistory(@NotNull UUID buyerId, int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<IOrder> list = new ArrayList<>();
            int offset = Math.max(page, 0) * pageSize;
            String sql = "SELECT * FROM " + database.getTablePrefix() + "orders_history WHERE buyer_id = ? "
                    + "ORDER BY archived_at DESC " + database.getPaginationClause(pageSize, offset);
            try (Connection conn = database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, buyerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while loading order history for buyer " + buyerId, e);
                throw new RuntimeException("Failed to load order history for buyer " + buyerId, e);
            }
        });
    }

    /**
     * Archives a batch of terminal orders from active storage to order history.
     *
     * <p>The move is performed within a single transaction so that orders are
     * not deleted unless they have been successfully inserted into history.</p>
     *
     * @param limit the maximum number of orders to archive
     * @return a future containing the number of archived orders
     */
    @Override
    @NotNull
    public CompletableFuture<Integer> archiveCompletedOrders(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String prefix = database.getTablePrefix();
            try (Connection conn = database.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    List<String> batchIds = new ArrayList<>();
                    String selectSql = "SELECT id FROM " + prefix + "orders "
                            + "WHERE order_status IN ('COMPLETED','CANCELLED','EXPIRED') LIMIT ?";
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        ps.setInt(1, limit);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) batchIds.add(rs.getString("id"));
                        }
                    }

                    if (batchIds.isEmpty()) {
                        conn.commit();
                        return 0;
                    }

                    String placeholders = String.join(",", Collections.nCopies(batchIds.size(), "?"));

                    String insertSql = "INSERT INTO " + prefix + "orders_history "
                            + "(id, buyer_id, item_data, total_price, price_per_item, "
                            + "created_at, expires_at, order_status, amount, original_amount, "
                            + "delivered, collected, archived_at) "
                            + "SELECT id, buyer_id, item_data, total_price, price_per_item, "
                            + "created_at, expires_at, order_status, amount, original_amount, "
                            + "delivered, collected, ? "
                            + "FROM " + prefix + "orders WHERE id IN (" + placeholders + ")";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        int idx = 1;
                        database.bindTimestamp(ps, idx++, Instant.now());
                        for (String id : batchIds) {
                            ps.setString(idx++, id);
                        }
                        ps.executeUpdate();
                    }

                    String deleteSql = "DELETE FROM " + prefix + "orders WHERE id IN (" + placeholders + ")";
                    try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                        for (int i = 0; i < batchIds.size(); i++) {
                            ps.setString(i + 1, batchIds.get(i));
                        }
                        ps.executeUpdate();
                    }

                    conn.commit();
                    return batchIds.size();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while archiving completed orders", e);
                throw new RuntimeException("Database operation failed while archiving completed orders", e);
            }
        });
    }

    @NotNull
    private IOrder mapRow(@NotNull ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID buyerId = UUID.fromString(rs.getString("buyer_id"));
        ItemStack itemStack = ItemSerializer.fromBytes(rs.getBytes("item_data"));
        BigDecimal totalPrice = rs.getBigDecimal("total_price");
        BigDecimal pricePerItem = rs.getBigDecimal("price_per_item");

        Instant createdAt = database.readTimestamp(rs, "created_at");
        Instant expiresAt = database.readTimestamp(rs, "expires_at");

        OrderStatus status = OrderStatus.valueOf(rs.getString("order_status"));
        long amount = rs.getLong("amount");
        long originalAmount = rs.getLong("original_amount");
        long delivered = rs.getLong("delivered");
        long collected = rs.getLong("collected");

        return new Order(
                id, buyerId, itemStack, totalPrice,
                pricePerItem, createdAt,
                expiresAt, status, amount, originalAmount, delivered, collected
        );
    }
}