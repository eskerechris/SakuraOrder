package me.chris.sakuraOrder.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.chris.sakuraOrder.command.SubCommand;
import me.chris.sakuraOrder.util.OrderMaintenanceLock;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /order lock} command.
 */
public class LockCommand implements SubCommand {

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("lock")
                .requires(source -> source.getSender().hasPermission("sakuraorder.command.lock"))
                .executes(this::execute);
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }

        if (OrderMaintenanceLock.isLocked()) {
            OrderMaintenanceLock.unlock();
            player.sendMessage(Component.text("Order services have been unlocked", NamedTextColor.GREEN));
        } else {
            OrderMaintenanceLock.lock();
            player.sendMessage(Component.text("Order services have been locked", NamedTextColor.GREEN));
        }

        return 1;
    }
}