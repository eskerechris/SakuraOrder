package me.chris.sakuraOrder.command.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.command.SubCommand;
import me.chris.sakuraOrder.menu.PlayerOrderHistoryMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

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
                        Commands.argument("player | uuid", StringArgumentType.word())
                                .executes(this::execute)
                );
    }

    private int execute(@NotNull CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }

        String input = StringArgumentType.getString(context, "player | uuid");
        OfflinePlayer target = resolveTarget(input);

        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(plugin.getLangService().message("order.history-not-found",
                    Map.of("player", input)));
            return 0;
        }

        new PlayerOrderHistoryMenu(plugin, target.getUniqueId()).displayTo(player);
        return 1;
    }

    @Nullable
    private OfflinePlayer resolveTarget(@NotNull String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException e) {
            return Bukkit.getOfflinePlayerIfCached(input);
        }
    }
}