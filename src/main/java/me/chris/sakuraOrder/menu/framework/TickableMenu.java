package me.chris.sakuraOrder.menu.framework;

/**
 * Interface implemented by menus that require periodic tick updates / animations.
 */
public interface TickableMenu {

    /**
     * Executes a tick cycle for the menu.
     *
     * @param currentTick the current server tick timestamp
     */
    void tick(long currentTick);
}