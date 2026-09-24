package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.TextInputRequest;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.util.NumberParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;

public class AmountButton extends Button {

    private final OrderPlugin plugin;
    private final LangService lang;
    private final long currentAmount;
    private final BiConsumer<Player, Long> onAmountSet;

    public AmountButton(OrderPlugin plugin, int slot, long currentAmount,
                        @NotNull BiConsumer<Player, Long> onAmountSet) {
        super(slot);
        this.plugin = plugin;
        this.lang = plugin.getLangService();
        this.currentAmount = currentAmount;
        this.onAmountSet = onAmountSet;
    }

    private void openDialog(Player player) {
        plugin.getDialogService().openTextInput(player, new TextInputRequest(
                lang.menuMessage("new-order-menu.amount-button.dialog-title"),
                lang.menuMessage("new-order-menu.amount-button.dialog-prompt"),
                String.valueOf(currentAmount),
                0,
                new ItemStack(Material.CHEST),
                input -> {
                    long amount;

                    try {
                        amount = NumberParser.parseLong(input);

                        if (amount > NumberParser.parseLong(
                                plugin.getSettingsService().getGeneral().getOrderMaxItemAmount())) {
                            player.sendMessage(lang.menuMessage("new-order-menu.amount-button.too-large"));
                            openDialog(player);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(lang.menuMessage("new-order-menu.amount-button.invalid"));
                        openDialog(player);
                        return;
                    }

                    if (amount <= 0) {
                        player.sendMessage(lang.menuMessage("new-order-menu.amount-button.must-be-positive"));
                        openDialog(player);
                        return;
                    }

                    SakuraOrder.getInstance().getSchedulerService().runAtEntity(
                            player,
                            () -> onAmountSet.accept(player, amount)
                    );
                },
                () -> {
                }
        ));
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.CHEST);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(lang.menuMessage("new-order-menu.amount-button.name"));
        meta.lore(lang.menuMessageList(
                "new-order-menu.amount-button.lore",
                Map.of("amount", NumberParser.formatNumber(currentAmount))
        ));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        openDialog(player);
    }
}