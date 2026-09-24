package me.chris.sakuraOrder.menu.framework;

/**
 * A button whose state can be updated dynamically.
 */
public abstract class DynamicButton extends Button {

    public DynamicButton(int slot) {
        super(slot);
    }

    /**
     * Updates the button's current state.
     */
    public abstract void update();
}