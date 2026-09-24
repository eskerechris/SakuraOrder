package me.chris.sakuraOrder.menu.framework;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClickAction {
    void onClick(@NotNull Player player, @NotNull ClickType clickType);
}