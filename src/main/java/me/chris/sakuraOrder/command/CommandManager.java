package me.chris.sakuraOrder.command;

import me.chris.sakuraOrder.api.OrderPlugin;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

/**
 * Manages registration of all plugin commands via Paper Brigadier API.
 */
public final class CommandManager {

    private final OrderPlugin plugin;

    public CommandManager(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all command handlers into the registrar.
     *
     * @param commands the Paper {@link Commands} registrar
     */
    public void registerCommands(@NotNull final Commands commands) {
        new OrderCommand(plugin).register(commands);
    }
}
