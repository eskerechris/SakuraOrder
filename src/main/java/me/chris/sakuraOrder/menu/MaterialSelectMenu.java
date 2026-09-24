package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.MaterialFilter;
import me.chris.sakuraOrder.api.model.MaterialSort;
import me.chris.sakuraOrder.api.model.OrderableItem;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.menu.components.FilterButton;
import me.chris.sakuraOrder.menu.components.SearchButton;
import me.chris.sakuraOrder.menu.components.SortButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.PaginatedMenu;
import me.chris.sakuraOrder.util.OrderableItemCatalog;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

public class MaterialSelectMenu extends PaginatedMenu<OrderableItem> {

    private final LangService lang = getPlugin().getLangService();
    private final BiConsumer<Player, OrderableItem> onSelect;

    private static final int ITEM_SLOT_START = 0;
    private static final int ITEM_SLOT_END = 44;
    private static final int SORT_BUTTON_SLOT = 48;
    private static final int SEARCH_BUTTON_SLOT = 49;
    private static final int FILTER_BUTTON_SLOT = 50;

    private MaterialFilter currentFilter = MaterialFilter.ALL;
    private MaterialSort currentSort = MaterialSort.A_TO_Z;
    private String currentSearch = "";

    public MaterialSelectMenu(@NotNull OrderPlugin plugin, @NotNull BiConsumer<Player, OrderableItem> onSelect) {
        super(plugin);
        this.onSelect = onSelect;

        setTitle(lang.menuMessage("material-select-menu.title",
                Map.of("current_page", String.valueOf(getCurrentPage() + 1),
                        "total_pages", String.valueOf(getTotalPages()))));
        setSize(9 * 6);
    }

    /**
     * The catalogue (~1000 entries, including potions and expanded books) is recalculated
     * every time the page is refreshed or a filter is changed.
     */
    private List<OrderableItem> filteredItems() {
        var blacklist = getPlugin().getMaterialBlacklistService();
        return OrderableItemCatalog.build(blacklist).stream()
                .filter(entry -> currentFilter.predicate().test(entry.baseMaterial()))
                .filter(entry -> matchesSearch(entry, currentSearch))
                .sorted(currentSort.comparator())
                .toList();
    }

    private static boolean matchesSearch(@NotNull OrderableItem entry, @NotNull String search) {
        if (search.isBlank()) return true;
        return entry.displayName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    @Override
    protected int getTotalItemCount() {
        return filteredItems().size();
    }

    @Override
    @NotNull
    protected List<OrderableItem> getItems(int page, int pageSize) {
        List<OrderableItem> items = filteredItems();
        int fromIndex = Math.min(page * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        return items.subList(fromIndex, toIndex);
    }

    @Override
    @NotNull
    protected Button createButtonFor(int slot, @NotNull OrderableItem entry) {
        return Button.of(slot, buildEntryItem(entry), (player, click) -> {
            if (EnchantSelectMenu.isEnchantable(entry.baseMaterial())) {
                new EnchantSelectMenu(getPlugin(), entry.baseMaterial(), onSelect).displayTo(player);
            } else {
                onSelect.accept(player, entry);
            }
        });
    }

    private ItemStack buildEntryItem(@NotNull OrderableItem entry) {
        ItemStack item = entry.itemStack().clone();
        var meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(lang.menuMessage("material-select-menu.item-button.name",
                Map.of("material", entry.displayName())));
        meta.lore(lang.menuMessageList("material-select-menu.item-button.lore"));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected int getItemSlotStart() {
        return ITEM_SLOT_START;
    }

    @Override
    protected int getItemSlotEnd() {
        return ITEM_SLOT_END;
    }

    @Override
    protected void registerExtraButtons() {
        addButton(new SortButton<>(getPlugin(), SORT_BUTTON_SLOT,
                () -> currentSort,
                this::changeSort));
        addButton(new FilterButton<>(getPlugin(), FILTER_BUTTON_SLOT,
                () -> currentFilter,
                this::changeFilter));
        addButton(new SearchButton(getPlugin(), SEARCH_BUTTON_SLOT,
                this::changeSearch));
    }

    private void changeSort(@NotNull MaterialSort newSort) {
        this.currentSort = newSort;
        changePage(0);
    }

    private void changeFilter(@NotNull MaterialFilter newFilter) {
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
        return lang.menuMessage("material-select-menu.title",
                Map.of("current_page", String.valueOf(current),
                        "total_pages", String.valueOf(total)));
    }

}