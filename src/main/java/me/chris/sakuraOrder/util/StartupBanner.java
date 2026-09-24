package me.chris.sakuraOrder.util;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Prints the startup banner in the server console.
 */
public final class StartupBanner {

    private static final String PINK = "\u00A7d";
    private static final String GRAY = "\u00A77";
    private static final String WHITE = "\u00A7f";

    private static final String[] ART = {
            " ____        _                     ___          _",
            "/ ___|  __ _| | ___   _ _ __ __ _ / _ \\ _ __ __| | ___ _ __",
            "\\___ \\ / _` | |/ / | | | '__/ _` | | | | '__/ _` |/ _ \\ '__|",
            " ___) | (_| |   <| |_| | | | (_| | |_| | | | (_| |  __/ |",
            "|____/ \\__,_|_|\\_\\\\__,_|_|  \\__,_|\\___/|_|  \\__,_|\\___|_|"
    };

    private StartupBanner() {}

    public static void print(@NotNull JavaPlugin plugin, @NotNull FoliaLib foliaLib) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        console.sendMessage("");
        for (String line : ART) {
            console.sendMessage(PINK + line);
        }
        console.sendMessage("");
        console.sendMessage(GRAY + "  Version: " + WHITE + plugin.getPluginMeta().getVersion()
        + GRAY + " Running on " + WHITE + platformName(foliaLib));
        console.sendMessage("");
    }

    @NotNull
    private static String platformName(@NotNull FoliaLib foliaLib) {
        if (foliaLib.isFolia()) {
            return "Folia";
        }
        if (foliaLib.isPaper()) {
            return "Paper";
        }
        if (foliaLib.isSpigot()) {
            return "Spigot";
        }
        return "Bukkit";
    }
}