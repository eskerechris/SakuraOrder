package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderFilter;
import me.chris.sakuraOrder.api.model.OrderSort;
import me.chris.sakuraOrder.menu.components.FilterButton;
import me.chris.sakuraOrder.menu.components.HistoryOrderButton;
import me.chris.sakuraOrder.menu.components.SearchButton;
import me.chris.sakuraOrder.menu.components.SortButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.PaginatedMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerOrderHistoryMenu extends PaginatedMenu<IOrder> {

    private static final int ORDER_SLOT_START = 0;
    private static final int ORDER_SLOT_END = 44;

    private static final int SORT_BUTTON_SLOT = 48;
    private static final int SEARCH_BUTTON_SLOT = 49;
    private static final int FILTER_BUTTON_SLOT = 50;

    private OrderFilter currentFilter = OrderFilter.ALL;
    private OrderSort currentSort = OrderSort.RECENTLY_LISTED;
    private String currentSearch = "";

    private final UUID buyerId;
    private List<IOrder> orders = List.of();

    /**
     * Incremented on each reload. Responses from older generations are discarded
     * so that a slow request cannot overwrite newer state. Accessed only from the
     * viewer's thread through button clicks and player scheduler callbacks.
     */
    private int reloadGeneration;

    public PlayerOrderHistoryMenu(@NotNull OrderPlugin plugin, @NotNull UUID buyerId) {
        super(plugin);
        this.buyerId = buyerId;
        setSize(9 * 6);
        setTitle(pageTitle(1, 1));
    }

    @Override
    public void displayTo(@NotNull Player player) {
        super.displayTo(player);
        reload(player);
    }

    @Override
    protected int getTotalItemCount() {
        return orders.size();
    }

    @Override
    @NotNull
    protected List<IOrder> getItems(int page, int pageSize) {
        int from = Math.min(page * pageSize, orders.size());
        int to = Math.min(from + pageSize, orders.size());
        return orders.subList(from, to);
    }

    @Override
    @NotNull
    protected Button createButtonFor(int slot, @NotNull IOrder order) {
        return new HistoryOrderButton(slot, order, getPlugin());
    }

    @Override
    protected int getItemSlotStart() {
        return ORDER_SLOT_START;
    }

    @Override
    protected int getItemSlotEnd() {
        return ORDER_SLOT_END;
    }

    @Override
    @NotNull
    protected Component pageTitle(int current, int total) {
        String playerName = Bukkit.getOfflinePlayer(buyerId).getName();
        return getPlugin().getLangService().menuMessage("player-order-history-menu.title",
                Map.of("current_page", String.valueOf(current),
                        "total_pages", String.valueOf(total),
                        "player_name", playerName != null ? playerName : "Unknown"));
    }

    @Override
    protected void registerExtraButtons() {
        addButton(new SortButton<>(getPlugin(),
                SORT_BUTTON_SLOT,
                () -> currentSort,
                this::changeSort));

        addButton(new SearchButton(getPlugin(), SEARCH_BUTTON_SLOT, this::changeSearch));

        addButton(new FilterButton<>(getPlugin(),
                FILTER_BUTTON_SLOT,
                () -> currentFilter,
                this::changeFilter));
    }

    private void changeSort(@NotNull OrderSort newSort) {
        this.currentSort = newSort;
        changePage(0);
        if (getViewer() != null) {
            reload(getViewer());
        }
    }

    private void changeFilter(@NotNull OrderFilter newFilter) {
        this.currentFilter = newFilter;
        changePage(0);
        if (getViewer() != null) {
            reload(getViewer());
        }
    }

    private void changeSearch(@NotNull String newSearch) {
        this.currentSearch = newSearch;
        changePage(0);
        if (getViewer() != null) {
            reload(getViewer());
        }
    }

    private void reload(@NotNull Player player) {
        int generation = ++reloadGeneration;

        getPlugin().getOrderHistoryService().getPlayerHistory(
                buyerId,
                currentFilter,
                currentSort,
                currentSearch
        ).whenComplete((loadedOrders, error) -> SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> {
            if (getViewer() != player || generation != reloadGeneration) {
                return;
            }
            if (error != null) {
                getPlugin().getLogger().log(Level.WARNING,
                        "Unable to load order history for buyer " + buyerId, error);
                return;
            }

            orders = List.copyOf(loadedOrders);
            clearButtons();
            registerButtons();
            setTitle(pageTitle(getCurrentPage() + 1, getTotalPages()));
            updateInventory();
            reopenForTitleChange();
        }));
    }
}