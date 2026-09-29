package me.chris.sakuraOrder.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.command.SubCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /order toggle} command.
 */
public final class ToggleNotificationCommand implements SubCommand {

    private final OrderPlugin plugin;

    public ToggleNotificationCommand(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("togglenotifications")
                .executes(this::execute);
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }

        boolean nowEnabled = plugin.getOrderDeliveryService().toggleNotification(player.getUniqueId());

        player.sendMessage(plugin.getLangService().message(
                nowEnabled ? "order.delivery-notifications-enabled" : "order.delivery-notifications-disabled"
        ));

        return 1;
    }
}