package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.TextInputRequest;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.util.NumberParser;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiConsumer;

public class PriceButton extends Button {

    private final OrderPlugin plugin;
    private final LangService lang;
    private final BigDecimal currentPrice;
    private final BiConsumer<Player, BigDecimal> onPriceSet;

    public PriceButton(OrderPlugin plugin, int slot, @Nullable BigDecimal currentPrice,
                       @NotNull BiConsumer<Player, BigDecimal> onPriceSet) {
        super(slot);
        this.plugin = plugin;
        this.lang = plugin.getLangService();
        this.currentPrice = currentPrice;
        this.onPriceSet = onPriceSet;
    }

    private void openDialog(Player player) {
        plugin.getDialogService().openTextInput(player, new TextInputRequest(
                lang.menuMessage("new-order-menu.price-button.dialog-title"),
                lang.menuMessage("new-order-menu.price-button.dialog-prompt"),
                currentPrice != null ? NumberParser.formatNumber(currentPrice) : "",
                0,
                new ItemStack(Material.EMERALD),
                input -> {
                    BigDecimal price;

                    try {
                        price = NumberParser.parseNumber(input);
                    } catch (NumberFormatException e) {
                        player.sendMessage(lang.menuMessage("new-order-menu.price-button.invalid"));
                        openDialog(player);
                        return;
                    }

                    if (price.compareTo(BigDecimal.ZERO) <= 0) {
                        player.sendMessage(lang.menuMessage("new-order-menu.price-button.must-be-positive"));
                        openDialog(player);
                        return;
                    }

                    SakuraOrder.getInstance().getSchedulerService().runAtEntity(
                            player,
                            () -> onPriceSet.accept(player, price)
                    );
                },
                () -> {
                }
        ));
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.EMERALD);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        String price = currentPrice != null
                ? NumberParser.formatNumber(currentPrice)
                : PlainTextComponentSerializer.plainText().serialize(
                lang.menuMessage("new-order-menu.not-set")
        );

        meta.displayName(lang.menuMessage("new-order-menu.price-button.name"));
        meta.lore(lang.menuMessageList(
                "new-order-menu.price-button.lore",
                Map.of("price", price)
        ));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        openDialog(player);
    }
}