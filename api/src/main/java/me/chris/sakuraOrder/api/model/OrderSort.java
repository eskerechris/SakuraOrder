package me.chris.sakuraOrder.api.model;

import java.util.Comparator;

/**
 * Defines the available sorting criteria for orders.
 *
 * <p>Each sort criterion provides a comparator used to determine the order
 * in which matching orders are displayed.</p>
 */
public enum OrderSort {

    /**
     * Sorts orders by creation time, with the most recently listed orders first.
     */
    RECENTLY_LISTED(Comparator.comparing(IOrder::getCreatedAt).reversed()),

    /**
     * Sorts orders by delivered amount, with orders having the most delivered
     * items first.
     */
    MOST_DELIVERED(Comparator.comparing(IOrder::getDelivered).reversed()),

    /**
     * Sorts orders by total amount paid, with the highest amounts first.
     */
    MOST_PAID(Comparator.comparing(IOrder::getAmountPaid).reversed()),

    /**
     * Sorts orders by price per item, with the highest prices first.
     */
    HIGHEST_PRICE_PER_ITEM(Comparator.comparing(IOrder::getPricePerItem).reversed());

    private final Comparator<IOrder> comparator;

    OrderSort(Comparator<IOrder> comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the comparator used to sort orders according to this criterion.
     *
     * @return the order sorting comparator
     */
    public Comparator<IOrder> comparator() {
        return comparator;
    }
}