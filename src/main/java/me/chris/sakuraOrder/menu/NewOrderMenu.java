package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.OrderableItem;
import me.chris.sakuraOrder.api.services.order.result.CreateResult;
import me.chris.sakuraOrder.menu.components.AmountButton;
import me.chris.sakuraOrder.menu.components.MaterialButton;
import me.chris.sakuraOrder.menu.components.PriceButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.model.Order;
import me.chris.sakuraOrder.util.NumberParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class NewOrderMenu extends Menu {

    private static final int CANCEL_SLOT = 10;
    private static final int MATERIAL_SLOT = 12;
    private static final int AMOUNT_SLOT = 13;
    private static final int PRICE_SLOT = 14;
    private static final int CONFIRM_SLOT = 16;

    private final OrderPlugin plugin;

    private OrderableItem selectedItem;
    private BigDecimal selectedPrice;
    private long selectedAmount = 1L;

    public NewOrderMenu(OrderPlugin plugin) {
        super(plugin);
        this.plugin = plugin;

        setTitle(plugin.getLangService()
                .menuMessage("new-order-menu.title"));

        setSize(9 * 3);
    }

    @Override
    protected void registerButtons() {
        addButton(Button.of(
                CANCEL_SLOT,
                buildCancelItem(),
                (player, click) -> new YourOrdersMenu(plugin).displayTo(player)
        ));

        addButton(new MaterialButton(plugin, MATERIAL_SLOT, selectedItem,
                (p, item) -> {
                    setSelectedItem(item);
                    displayTo(p);
                }));

        addButton(new PriceButton(plugin, PRICE_SLOT, selectedPrice,
                (p, price) -> {
                    setSelectedPrice(price);
                    displayTo(p);
                }));

        addButton(new AmountButton(plugin, AMOUNT_SLOT, selectedAmount,
                (p, amount) -> {
                    setSelectedAmount(amount);
                    displayTo(p);
                }));

        addButton(Button.of(
                CONFIRM_SLOT,
                buildConfirmItem(),
                (player, click) -> onConfirm(player)
        ));
    }

    public void setSelectedItem(OrderableItem item) {
        this.selectedItem = item;
        refresh();
    }

    public void setSelectedPrice(BigDecimal price) {
        this.selectedPrice = price;
        refresh();
    }

    public void setSelectedAmount(long amount) {
        this.selectedAmount = Math.max(1, amount);
        refresh();
    }

    private void refresh() {
        addButton(new MaterialButton(plugin, MATERIAL_SLOT, selectedItem,
                (p, item) -> {
                    setSelectedItem(item);
                    displayTo(p);
                }));

        addButton(new PriceButton(plugin, PRICE_SLOT, selectedPrice,
                (p, price) -> {
                    setSelectedPrice(price);
                    displayTo(p);
                }));

        addButton(new AmountButton(plugin, AMOUNT_SLOT, selectedAmount,
                (p, amount) -> {
                    setSelectedAmount(amount);
                    displayTo(p);
                }));

        addButton(Button.of(
                CONFIRM_SLOT,
                buildConfirmItem(),
                (player, click) -> onConfirm(player)
        ));

        updateInventory();
    }

    private ItemStack buildCancelItem() {
        var item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(plugin.getLangService().menuMessage("new-order-menu.cancel-button.name"));
        meta.lore(plugin.getLangService().menuMessageList("new-order-menu.cancel-button.lore"));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmItem() {
        var item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        String totalPrice = selectedPrice != null
                ? NumberParser.formatNumber(
                selectedPrice.multiply(
                        BigDecimal.valueOf(selectedAmount)
                )
        )
                : "-";

        meta.displayName(plugin.getLangService().menuMessage("new-order-menu.confirm-button.name"));

        meta.lore(plugin.getLangService().menuMessageList(
                "new-order-menu.confirm-button.lore",
                Map.of("total_price", totalPrice)
        ));

        item.setItemMeta(meta);
        return item;
    }

    private void onConfirm(Player player) {
        if (selectedItem == null || selectedPrice == null) {
            return;
        }

        plugin.getOrderCreateService()
                .createOrder(
                        Order.builder()
                                .buyerId(player.getUniqueId())
                                .itemStack(selectedItem.itemStack().clone())
                                .pricePerItem(selectedPrice)
                                .amount(selectedAmount)
                                .totalPrice(selectedPrice.multiply(BigDecimal.valueOf(selectedAmount)))
                                .expiresAt(Instant.now().plus(
                                        plugin.getSettingsService().getGeneral().getOrderExpiration(),
                                        ChronoUnit.DAYS))
                                .build())
                .thenAccept(result -> SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> {
                    if (result instanceof CreateResult.Created created) {
                        player.sendMessage(created.message());
                        player.closeInventory();
                    } else if (result instanceof CreateResult.Failed failed) {
                        player.sendMessage(failed.message());
                        player.closeInventory();
                    }
                }));
    }
}