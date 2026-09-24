package me.chris.sakuraOrder.menu.framework;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.menu.components.NavigationButton;
import me.chris.sakuraOrder.menu.components.RefreshButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base class for menus that display items across multiple pages.
 *
 * @param <T> the type of item displayed in the menu
 */
public abstract class PaginatedMenu<T> extends Menu {

    private static final int PREV_PAGE_SLOT = 45;
    private static final int REFRESH_BUTTON_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private int currentPage = 0;

    protected PaginatedMenu(@NotNull OrderPlugin plugin) {
        super(plugin);
    }

    /**
     * Gets the total number of items available.
     *
     * @return the total item count
     */
    protected abstract int getTotalItemCount();

    /**
     * Gets the items to display on the specified page.
     *
     * @param page the zero-based page index
     * @param pageSize the maximum number of items to return
     * @return the items for the requested page
     */
    @NotNull
    protected abstract List<T> getItems(int page, int pageSize);

    /**
     * Creates a button for the specified item.
     *
     * @param slot the inventory slot for the button
     * @param item the item to display
     * @return the button representing the item
     */
    @NotNull
    protected abstract Button createButtonFor(int slot, @NotNull T item);

    /**
     * Gets the first slot available for page items.
     *
     * @return the first item slot
     */
    protected abstract int getItemSlotStart();

    /**
     * Gets the last slot available for page items.
     *
     * @return the last item slot
     */
    protected abstract int getItemSlotEnd();

    protected int getPrevPageSlot() {
        return PREV_PAGE_SLOT;
    }

    protected int getNextPageSlot() {
        return NEXT_PAGE_SLOT;
    }

    protected int getRefreshButtonSlot() {
        return REFRESH_BUTTON_SLOT;
    }

    /**
     * Creates the title displayed for the specified page.
     *
     * @param current the current page number, starting from one
     * @param total the total number of pages
     * @return the menu title
     */
    @NotNull
    protected abstract Component pageTitle(int current, int total);

    /**
     * Called after the buttons for the current page have been registered.
     *
     * @param pageItems the items displayed on the current page
     */
    protected void onPageRegistered(@NotNull List<T> pageItems) {}

    /**
     * Registers additional buttons provided by the menu.
     */
    protected void registerExtraButtons() {}

    protected final int getCurrentPage() {
        return currentPage;
    }

    protected final int getItemsPerPage() {
        return getItemSlotEnd() - getItemSlotStart() + 1;
    }

    protected final int getTotalPages() {
        return Math.max(1, (int) Math.ceil(getTotalItemCount() / (double) getItemsPerPage()));
    }

    @Override
    protected void registerButtons() {
        int totalPages = getTotalPages();
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        List<T> pageItems = getItems(currentPage, getItemsPerPage());
        for (int i = 0; i < pageItems.size(); i++) {
            addButton(createButtonFor(getItemSlotStart() + i, pageItems.get(i)));
        }

        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;

        addButton(new NavigationButton(getPlugin(), getPrevPageSlot(), false, hasPrev, () -> changePage(currentPage - 1)));
        addButton(new NavigationButton(getPlugin(), getNextPageSlot(), true, hasNext, () -> changePage(currentPage + 1)));
        addButton(new RefreshButton(getPlugin(), getRefreshButtonSlot(), () -> changePage(0)));

        registerExtraButtons();
        onPageRegistered(pageItems);
    }

    protected final void changePage(int newPage) {
        this.currentPage = newPage;
        clearButtons();
        registerButtons();
        setTitle(pageTitle(currentPage + 1, getTotalPages()));
        updateInventory();
        reopenForTitleChange();
    }
}