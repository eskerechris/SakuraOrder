package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.model.OrderFilter;
import me.chris.sakuraOrder.api.model.OrderSort;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.menu.components.FilterButton;
import me.chris.sakuraOrder.menu.components.OrderButton;
import me.chris.sakuraOrder.menu.components.SearchButton;
import me.chris.sakuraOrder.menu.components.SortButton;
import me.chris.sakuraOrder.menu.components.YourOrdersButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.PaginatedMenu;
import me.chris.sakuraOrder.menu.framework.TickableMenu;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class OrderMenu extends PaginatedMenu<IOrder> implements TickableMenu {

    private final LangService lang = getPlugin().getLangService();

    private static final int ORDER_SLOT_START = 0;
    private static final int ORDER_SLOT_END = 44;
    private static final int SORT_BUTTON_SLOT = 47;
    private static final int FILTER_BUTTON_SLOT = 48;
    private static final int SEARCH_BUTTON_SLOT = 50;
    private static final int YOUR_ORDERS_BUTTON_SLOT = 51;

    private Set<UUID> lastPageOrderIds = new LinkedHashSet<>();

    private OrderFilter currentFilter = OrderFilter.ALL;
    private OrderSort currentSort = OrderSort.RECENTLY_LISTED;
    private String currentSearch = "";

    private WrappedTask wrappedTask;

    public OrderMenu(@NotNull OrderPlugin plugin) {
        super(plugin);
        setTitle(lang.menuMessage("order-menu.title",
                Map.of("current_page", String.valueOf(getCurrentPage() + 1),
                        "total_pages", String.valueOf(getTotalPages()))));
        setSize(9 * 6);
    }

    public OrderMenu(@NotNull OrderPlugin plugin, @NotNull String initialSearch) {
        super(plugin);
        this.currentSearch = initialSearch;
        setTitle(lang.menuMessage("order-menu.title",
                Map.of("current_page", String.valueOf(getCurrentPage() + 1),
                        "total_pages", String.valueOf(getTotalPages()))));
        setSize(9 * 6);
    }

    @Override
    protected int getTotalItemCount() {
        return getPlugin().getOrderCacheService().getActiveCount(currentFilter, currentSearch);
    }

    @Override
    @NotNull
    protected List<IOrder> getItems(int page, int pageSize) {
        return getPlugin().getOrderCacheService().getPage(page, pageSize, currentFilter, currentSort, currentSearch);
    }

    @Override
    @NotNull
    protected Button createButtonFor(int slot, @NotNull IOrder order) {
        return new OrderButton(slot, order.getId(), this, getPlugin());
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
    protected void registerExtraButtons() {
        addButton(new SortButton<>(getPlugin(), SORT_BUTTON_SLOT,
                () -> currentSort,
                this::changeSort));
        addButton(new FilterButton<>(getPlugin(), FILTER_BUTTON_SLOT,
                () -> currentFilter,
                this::changeFilter));
        addButton(new SearchButton(getPlugin(), SEARCH_BUTTON_SLOT, this::changeSearch));

        addButton(new YourOrdersButton(getPlugin(), YOUR_ORDERS_BUTTON_SLOT));
    }

    private void changeSort(@NotNull OrderSort newSort) {
        this.currentSort = newSort;
        changePage(0);
    }

    private void changeFilter(@NotNull OrderFilter newFilter) {
        this.currentFilter = newFilter;
        changePage(0);
    }

    private void changeSearch(@NotNull String newSearch) {
        this.currentSearch = newSearch;
        changePage(0);
    }

    @Override
    @NotNull
    protected Component pageTitle(int current, int total) {
        return lang.menuMessage("order-menu.title",
                Map.of("current_page", String.valueOf(current),
                        "total_pages", String.valueOf(total)));
    }

    @Override
    protected void onPageRegistered(@NotNull List<IOrder> pageItems) {
        this.lastPageOrderIds = pageItems.stream()
                .map(IOrder::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    protected void updateInventory() {
        Inventory inv = getInventory();
        for (int slot = ORDER_SLOT_START; slot <= ORDER_SLOT_END; slot++) {
            inv.setItem(slot, null);
        }
        super.updateInventory();
    }

    @Override
    public void displayTo(@NotNull Player player) {
        super.displayTo(player);
        cancelTask();
        wrappedTask = SakuraOrder.getInstance().getSchedulerService().runAtEntityTimer(
                player, () -> this.tick(player.getServer().getCurrentTick()), 20L, 20L
        );
    }

    @Override
    public void tick(long currentTick) {
        List<IOrder> freshPageOrders = getPlugin().getOrderCacheService()
                .getPage(getCurrentPage(), getItemsPerPage(), currentFilter, currentSort, currentSearch);
        Set<UUID> freshIds = freshPageOrders.stream()
                .map(IOrder::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!freshIds.equals(lastPageOrderIds)) {
            clearButtons();
            registerButtons();
            updateInventory();
            return;
        }

        updateDynamicButtons();
        super.updateInventory();
    }

    @Override
    public void onClose(@NotNull Player player) {
        super.onClose(player);
        cancelTask();
    }

    private void cancelTask() {
        if (wrappedTask != null) {
            wrappedTask.cancel();
            wrappedTask = null;
        }
    }
}