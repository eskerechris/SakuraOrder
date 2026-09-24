package me.chris.sakuraOrder.services.gui;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.gui.GuiService;
import me.chris.sakuraOrder.menu.EditOrderMenu;
import me.chris.sakuraOrder.menu.OrderMenu;
import me.chris.sakuraOrder.menu.YourOrdersMenu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PluginGuiService implements GuiService {

    private final OrderPlugin plugin;

    public PluginGuiService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void openOrderMenu(@NotNull Player player) {
        new OrderMenu(plugin).displayTo(player);
    }

    @Override
    public void openYoursOrderMenu(@NotNull Player player) {
        new YourOrdersMenu(plugin).displayTo(player);
    }

    @Override
    public void openEditOrderMenu(@NotNull Player player, @NotNull UUID orderId) {
        new EditOrderMenu(plugin, orderId).displayTo(player);
    }

}