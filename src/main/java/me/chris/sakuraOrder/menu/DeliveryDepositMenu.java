package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.util.DeliveryCalculator;
import me.chris.sakuraOrder.util.OrderMaintenanceLock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menu used to deposit items for a pending order delivery.
 */
public class DeliveryDepositMenu extends Menu {

    private final UUID orderId;

    public DeliveryDepositMenu(@NotNull OrderPlugin plugin, @NotNull UUID orderId) {
        super(plugin);
        this.orderId = orderId;
        setSize(9 * 4);
        setTitle(plugin.getLangService().menuMessage("delivery-deposit-menu.title"));
    }

    @Override
    protected void registerButtons() {}

    @Override
    public void onClose(@NotNull Player player) {
        List<ItemStack> deposited = new ArrayList<>();
        for (ItemStack stack : getInventory().getContents()) {
            if (stack != null && !stack.getType().isAir()) {
                deposited.add(stack.clone());
            }
        }

        super.onClose(player);

        if (deposited.isEmpty()) {
            return;
        }

        IOrder order = getPlugin().getOrderCacheService().get(orderId);
        if (order == null) {
            returnImmediately(deposited, player);
            return;
        }

        if (OrderMaintenanceLock.isLocked()) {
            returnImmediately(deposited, player);
            player.sendMessage(getPlugin().getLangService().message("order.locked"));
            return;
        }

        DeliveryCalculator.DepositResolution resolution = DeliveryCalculator.resolveDeposit(order, deposited);

        if (!resolution.toReturnNow().isEmpty()) {
            returnImmediately(resolution.toReturnNow(), player);
        }

        List<ItemStack> toDeliver = resolution.toDeliver();
        if (toDeliver.isEmpty()) {
            return;
        }

        // Open menu next tick to avoid infinite loop
        SakuraOrder.getInstance().getSchedulerService().runAtEntityLater(player, () ->
                new DeliveryConfirmMenu(getPlugin(), orderId, toDeliver).displayTo(player), 1L
        );
    }

    private void returnImmediately(@NotNull List<ItemStack> stacks, @NotNull Player player) {
        var leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
        leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }
}