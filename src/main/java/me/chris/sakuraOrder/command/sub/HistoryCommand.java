package me.chris.sakuraOrder.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.command.SubCommand;
import me.chris.sakuraOrder.menu.PlayerOrderHistoryMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /order history} command.
 */
public final class HistoryCommand implements SubCommand {

    private final OrderPlugin plugin;

    public HistoryCommand(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("history")
                .requires(source -> source.getSender().hasPermission("sakuraorder.command.history"))
                .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes(this::execute)
                );
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }

        String playerName = StringArgumentType.getString(context, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

        if (target == null) {
            player.sendMessage(Component.text("%s's history not found".formatted(playerName), NamedTextColor.RED));
            return 0;
        }

        new PlayerOrderHistoryMenu(plugin, target.getUniqueId()).displayTo(player);
        return 1;
    }
}