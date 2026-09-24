package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.order.OrderCollectService.CollectMode;
import me.chris.sakuraOrder.api.services.order.result.CollectResult;
import me.chris.sakuraOrder.menu.components.DropAllButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.PaginatedMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CollectMenu extends PaginatedMenu<Integer> {

    private static final int DROP_ALL_SLOT = 49;

    private final UUID orderId;

    public CollectMenu(@NotNull OrderPlugin plugin, @NotNull UUID orderId) {
        super(plugin);
        this.orderId = orderId;
        setSize(9 * 6);
        setTitle(getPlugin().getLangService().menuMessage("collect-menu.title",
                Map.of("current_page", String.valueOf(getCurrentPage() + 1),
                        "total_pages", String.valueOf(getTotalPages()))));
    }

    @Override
    protected int getItemSlotStart() {
        return 0;
    }

    @Override
    protected int getItemSlotEnd() {
        return 44;
    }

    @Override
    protected int getTotalItemCount() {
        return stackAmounts().size();
    }

    @Override
    @NotNull
    protected List<Integer> getItems(int page, int pageSize) {
        List<Integer> all = stackAmounts();
        int start = Math.min(page * pageSize, all.size());
        int end = Math.min(start + pageSize, all.size());
        return all.subList(start, end);
    }

    /**
     * Splits the order's current collectable amount into full-stack-sized chunks (the last
     * one possibly smaller). Recomputed on every call, both {@link #getTotalItemCount()} and
     * {@link #getItems} call it independently within the same {@code registerButtons()} pass,
     * so it always reflects live cache state rather than a value cached across page loads.
     */
    @NotNull
    private List<Integer> stackAmounts() {
        IOrder order = getPlugin().getOrderCacheService().get(orderId);
        if (order == null) {
            return List.of();
        }

        long collectable = order.getCollectableAmount();
        int maxStack = order.getItemStack().getType().getMaxStackSize();
        int fullStacks = (int) (collectable / maxStack);
        int remainder = (int) (collectable % maxStack);

        List<Integer> amounts = new ArrayList<>(fullStacks + (remainder > 0 ? 1 : 0));
        for (int i = 0; i < fullStacks; i++) {
            amounts.add(maxStack);
        }
        if (remainder > 0) {
            amounts.add(remainder);
        }
        return amounts;
    }

    @Override
    @NotNull
    protected Button createButtonFor(int slot, @NotNull Integer amount) {
        return new Button(slot) {
            @Override
            @NotNull
            protected ItemStack createItem() {
                IOrder order = getPlugin().getOrderCacheService().get(orderId);
                Material material = order != null ? order.getItemStack().getType() : Material.BARRIER;

                return new ItemStack(material, amount);
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                getPlugin().getOrderCollectService()
                        .collect(orderId, player, amount, CollectMode.TO_INVENTORY)
                        .thenAccept(result -> SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> {
                            switch (result) {
                                case CollectResult.Collected collected -> player.sendMessage(collected.message());
                                case CollectResult.Failed failed -> player.sendMessage(failed.message());
                            }
                            changePage(getCurrentPage());
                        }));
            }
        };
    }

    @Override
    protected void registerExtraButtons() {
        addButton(new DropAllButton(
                DROP_ALL_SLOT,
                orderId,
                getItems(getCurrentPage(), getItemsPerPage()),
                ignored -> changePage(getCurrentPage()),
                getPlugin()
        ));
    }

    @Override
    @NotNull
    protected Component pageTitle(int current, int total) {
        return getPlugin().getLangService().menuMessage("collect-menu.title",
                Map.of("current_page", String.valueOf(current),
                        "total_pages", String.valueOf(total)));
    }
}