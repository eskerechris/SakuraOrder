package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.menu.components.CreateOrderButton;
import me.chris.sakuraOrder.menu.components.OrderButton;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.util.OrderLimitResolver;

import java.util.Comparator;
import java.util.List;

public class YourOrdersMenu extends Menu {

    public YourOrdersMenu(OrderPlugin plugin) {
        super(plugin);
        setTitle(plugin.getLangService().menuMessage("your-orders-menu.title"));
        setSize(9 * 3);
    }

    @Override
    protected void registerButtons() {
        if (getViewer() == null) {
            getPlugin().getLogger().severe("Viewer is null for menu: " + this);
            return;
        }

        int configuredDefault = getPlugin().getSettingsService().getGeneral().getMaxOrderSupported();

        int limit = getViewer() != null
                ? OrderLimitResolver.resolve(getViewer(), configuredDefault)
                : configuredDefault;

        int activeOrders = getPlugin().getOrderCacheService().getActiveByBuyer(getViewer().getUniqueId()).size();
        boolean canCreate = activeOrders < limit;

        addButton(new CreateOrderButton(getPlugin(),
                26,
                canCreate));


        List<IOrder> orders = getPlugin().getOrderCacheService()
                .getCachedByBuyer(getViewer().getUniqueId()).stream()
                .sorted(Comparator.comparing(IOrder::getCreatedAt, Comparator.reverseOrder()))
                .limit(26)
                .toList();

        for (int i = 0; i < orders.size(); i++) {
            addButton(new OrderButton(i, orders.get(i).getId(), this, getPlugin()));
        }
    }
}