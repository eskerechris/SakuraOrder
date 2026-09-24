package me.chris.sakuraOrder.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.command.SubCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /order reload} command.
 */
public final class ReloadCommand implements SubCommand {

    private final OrderPlugin plugin;

    public ReloadCommand(OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("sakuraorder.command.reload"))
                .executes(context -> {
                    plugin.reload();
                    context.getSource().getSender().sendMessage(
                            Component.text("SakuraOrder configuration reloaded successfully.", NamedTextColor.GREEN)
                    );
                    return 1;
                });
    }
}