package me.chris.sakuraOrder.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represent a subcommand of a main command.
 */
public interface SubCommand {

    /**
     * @return the Brigadier command builder for this subcommand
     */
    @NotNull LiteralArgumentBuilder<CommandSourceStack> build();
}
