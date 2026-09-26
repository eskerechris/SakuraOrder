package me.chris.sakuraOrder.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the sounds used by SakuraOrder for different actions.
 */
public enum SakuraSound {

    /**
     * Sound played when changing a page.
     */
    PAGE_CHANGE("page-change"),

    /**
     * Sound played when interacting with a menu button.
     */
    BUTTON_INTERACT("button-interact"),

    /**
     * Sound played when an order is created.
     */
    ORDER_CREATE("order-create"),

    /**
     * Sound played when an order is cancelled.
     */
    ORDER_CANCEL("order-cancel")

    ;

    private final String configKey;

    SakuraSound(String configKey) {
        this.configKey = configKey;
    }

    /**
     * Returns the configuration key associated with this sound.
     *
     * @return the configuration key
     */
    public @NotNull String configKey() {
        return configKey;
    }
}