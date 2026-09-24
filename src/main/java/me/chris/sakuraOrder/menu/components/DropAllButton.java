package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.order.OrderCollectService;
import me.chris.sakuraOrder.api.services.order.result.CollectResult;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class DropAllButton extends Button {

    private final OrderPlugin plugin;
    private final UUID orderId;
    private final List<Integer> items;
    private final Consumer<Void> consumer;

    public DropAllButton(int slot, UUID orderId, List<Integer> items, Consumer<Void> consumer, OrderPlugin plugin) {
        super(slot);
        this.plugin = plugin;
        this.orderId = orderId;
        this.items = items;
        this.consumer = consumer;
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.DROPPER);
        var meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(plugin.getLangService().menuMessage("collect-menu.drop-all-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("collect-menu.drop-all-button.lore"));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        int totalOnPage = items.stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalOnPage <= 0) return;

        plugin.getOrderCollectService()
                .collect(
                        orderId,
                        player,
                        totalOnPage,
                        OrderCollectService.CollectMode.DROP_AT_FEET
                )
                .thenAccept(result -> SakuraOrder.getInstance().getSchedulerService().runAtEntity(
                        player,
                        () -> {
                            switch (result) {
                                case CollectResult.Collected collected ->
                                        player.sendMessage(collected.message());
                                case CollectResult.Failed failed ->
                                        player.sendMessage(failed.message());
                            }

                            consumer.accept(null);
                        }
                ));
    }
}