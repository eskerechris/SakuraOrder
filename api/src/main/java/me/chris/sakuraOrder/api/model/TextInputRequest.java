package me.chris.sakuraOrder.api.model;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Represents a request to open a text input dialog for a player.
 *
 * <p>The request contains the dialog's visual content, the initial input value,
 * the maximum input length, and callbacks invoked when the player confirms or
 * cancels the dialog.</p>
 *
 * @param title        the title displayed on the dialog
 * @param prompt       the prompt or label describing the expected input
 * @param initialValue the initial text pre-filled in the input field
 * @param maxLength    the maximum number of characters allowed in the input
 * @param item         the item associated with the input request
 * @param onConfirm    callback invoked with the raw input when the player confirms the dialog
 * @param onCancel     callback invoked when the player cancels or dismisses the dialog
 */
public record TextInputRequest(
        @NotNull Component title,
        @NotNull Component prompt,
        @NotNull String initialValue,
        int maxLength,
        @NotNull ItemStack item,
        @NotNull Consumer<String> onConfirm,
        @NotNull Runnable onCancel
) {}