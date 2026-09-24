package me.chris.sakuraOrder.util;

import me.chris.sakuraOrder.api.model.IOrder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Search matching shared by the marketplace paging */
public final class OrderSearch {

    private OrderSearch() {}

    public static boolean matches(@NotNull IOrder order, @NotNull String search) {
        if (search.isBlank()) return true;
        String materialName = order.getItemStack().getType().name().replace('_', ' ');
        return materialName.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }
}