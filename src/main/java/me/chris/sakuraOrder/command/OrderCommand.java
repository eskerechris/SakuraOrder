package me.chris.sakuraOrder.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.command.sub.*;
import me.chris.sakuraOrder.command.sub.*;
import me.chris.sakuraOrder.menu.OrderMenu;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers and handles the {@code /order} command.
 */
public final class OrderCommand {

    private final OrderPlugin plugin;
    private final List<SubCommand> subCommands;

    public OrderCommand(OrderPlugin plugin) {
        this.plugin = plugin;
        this.subCommands = List.of(
                new ReloadCommand(plugin),
                new CreateCommand(plugin),
                new HistoryCommand(plugin),
                new LockCommand(),
                new AdminCommand(plugin)
        );
    }

    public void register(@NotNull final Commands commands) {
        var root = Commands.literal("order")
                .executes(context -> {
                    if (context.getSource().getSender() instanceof Player player) {
                        new OrderMenu(plugin).displayTo(player);
                    } else {
                        context.getSource().getSender().sendMessage(
                                Component.text("This command can only be executed by players.", NamedTextColor.RED)
                        );
                    }
                    return 1;
                })
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (context.getSource().getSender() instanceof Player player) {
                                String search = StringArgumentType.getString(context, "search");
                                new OrderMenu(plugin, search).displayTo(player);
                            } else {
                                context.getSource().getSender().sendMessage(
                                        Component.text("This command can only be executed by players.", NamedTextColor.RED)
                                );
                            }
                            return 1;
                        }));

        subCommands.forEach(sub -> root.then(sub.build()));

        commands.register(root.build());
    }
}