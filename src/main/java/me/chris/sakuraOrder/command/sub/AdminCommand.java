package me.chris.sakuraOrder.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.command.SubCommand;
import me.chris.sakuraOrder.menu.AdminPanelMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /order admin} command.
 */
public class AdminCommand implements SubCommand {

    private final OrderPlugin plugin;

    public AdminCommand(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("admin")
                .requires(source -> source.getSender().hasPermission("sakuraorder.command.admin"))
                .executes(this::execute);
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }

        new AdminPanelMenu(plugin).displayTo(player);
        return 1;
    }
}