package me.chris.sakuraOrder.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.api.services.order.result.CreateResult;
import me.chris.sakuraOrder.command.SubCommand;
import me.chris.sakuraOrder.model.Order;
import me.chris.sakuraOrder.util.NumberParser;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handles the {@code /order create} command.
 */
public final class CreateCommand implements SubCommand {

    private final OrderPlugin plugin;
    private final LangService lang;

    public CreateCommand(OrderPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLangService();
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("create")
                .then(
                        Commands.argument("item", StringArgumentType.word())
                                .then(
                                        Commands.argument("amount", StringArgumentType.word())
                                                .then(
                                                        Commands.argument("price", StringArgumentType.word())
                                                                .executes(this::execute)
                                                )
                                )
                );
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by players.", NamedTextColor.RED));
            return 0;
        }

        final String rawItem = StringArgumentType.getString(context, "item");
        final String rawAmount = StringArgumentType.getString(context, "amount");
        final String rawPrice = StringArgumentType.getString(context, "price");

        final Material material = Material.matchMaterial(rawItem);
        if (material == null || !material.isItem()) {
            player.sendMessage(lang.message("order.invalid-item-material"));
            return 0;
        }

        final long amount;
        try {
            amount = NumberParser.parseLong(rawAmount);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.message("order.invalid-item-amount"));
            return 0;
        }

        final BigDecimal pricePerItem;
        try {
            pricePerItem = NumberParser.parseNumber(rawPrice).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.message("order.invalid-price-amount"));
            return 0;
        }

        final BigDecimal totalPrice = pricePerItem.multiply(BigDecimal.valueOf(amount));
        final ItemStack itemStack = new ItemStack(material, 1);

        final IOrder order = Order.builder()
                .buyerId(player.getUniqueId())
                .itemStack(itemStack)
                .amount(amount)
                .pricePerItem(pricePerItem)
                .totalPrice(totalPrice)
                .expiresAt(Instant.now().plus(plugin.getSettingsService().getGeneral().getOrderExpiration(), ChronoUnit.DAYS))
                .build();

        plugin.getOrderCreateService().createOrder(order).thenAccept(result ->
                SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> handleCreateResult(player, result, order))
        );

        return 1;
    }

    private void handleCreateResult(@NotNull Player player, @NotNull CreateResult result, @NotNull IOrder order) {
        if (result instanceof CreateResult.Created created) {
            player.sendMessage(created.message());
            return;
        }

        if (result instanceof CreateResult.Failed failed) {
            player.sendMessage(failed.message());
        }
    }
}