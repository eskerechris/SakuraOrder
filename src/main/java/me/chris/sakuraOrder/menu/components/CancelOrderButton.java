package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.api.services.order.result.FinalizeResult;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public class CancelOrderButton extends Button {

    private final OrderPlugin plugin;
    private final UUID orderId;
    private final Consumer<FinalizeResult> finalize;

    public CancelOrderButton(int slot, UUID orderId, Consumer<FinalizeResult> finalize, OrderPlugin plugin) {
        super(slot);
        this.plugin = plugin;
        this.orderId = orderId;
        this.finalize = finalize;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.RED_TERRACOTTA);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(plugin.getLangService().menuMessage("edit-order-menu.cancel-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("edit-order-menu.cancel-button.lore"));

        item.setItemMeta(meta);

        return item;
    }


    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        if (plugin.getOrderCacheService().get(orderId) == null) {
            return;
        }

        plugin.getOrderCancelService().finalizeEarly(orderId, player)
                .thenAccept(result -> SakuraOrder.getInstance().getSchedulerService().runAtEntity(
                        player,
                        () -> finalize.accept(result)
                ));
        plugin.getSoundService().play(player, SakuraSound.ORDER_CANCEL);
    }
}
