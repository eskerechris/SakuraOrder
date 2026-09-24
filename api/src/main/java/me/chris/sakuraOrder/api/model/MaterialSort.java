package me.chris.sakuraOrder.api.model;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Defines the available sorting orders for materials.
 *
 * <p>Each sort order provides a comparator based on the display name
 * of the corresponding {@link OrderableItem}.</p>
 */
public enum MaterialSort {

    /**
     * Sorts materials alphabetically from A to Z.
     */
    A_TO_Z(Comparator.comparing(OrderableItem::displayName, String.CASE_INSENSITIVE_ORDER)),

    /**
     * Sorts materials alphabetically from Z to A.
     */
    Z_TO_A(Comparator.comparing(OrderableItem::displayName, String.CASE_INSENSITIVE_ORDER).reversed());

    private final Comparator<OrderableItem> comparator;

    MaterialSort(Comparator<OrderableItem> comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the comparator used to sort materials according to this order.
     *
     * @return the material sorting comparator
     */
    @NotNull
    public Comparator<OrderableItem> comparator() {
        return comparator;
    }
}