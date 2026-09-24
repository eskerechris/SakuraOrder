package me.chris.sakuraOrder.services.dialog;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.dialog.DialogService;
import me.chris.sakuraOrder.api.model.TextInputRequest;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PaperDialogService implements DialogService {

    private static final String TEXT_INPUT_KEY = "input";
    private static final int DEFAULT_BUTTON_WIDTH = 120;
    private static final int DEFAULT_INPUT_WIDTH = 250;

    private final OrderPlugin plugin;

    public PaperDialogService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void openTextInput(@NotNull Player player, @NotNull TextInputRequest request) {
        Dialog dialog = Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();

            TextDialogInput.Builder inputBuilder = DialogInput.text(TEXT_INPUT_KEY, request.prompt())
                    .width(DEFAULT_INPUT_WIDTH);

            if (!request.initialValue().isEmpty()) {
                inputBuilder.initial(request.initialValue());
            }

            if (request.maxLength() > 0) {
                inputBuilder.maxLength(request.maxLength());
            }

            TextDialogInput textInput = inputBuilder.build();

            DialogBase dialogBase = DialogBase.builder(request.title())
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .body(List.of(
                            DialogBody.item(
                                    request.item(),
                                    null,
                                    false,
                                    false,
                                    16,
                                    16
                            )
                    ))
                    .inputs(List.of(textInput))
                    .build();

            Component confirmLabel = plugin.getLangService().menuMessage("dialog.generic.confirm");
            Component cancelLabel = plugin.getLangService().menuMessage("dialog.generic.cancel");

            ActionButton confirmButton = ActionButton.create(
                    confirmLabel,
                    null,
                    DEFAULT_BUTTON_WIDTH,
                    DialogAction.customClick((response, audience) -> {
                        String text = response.getText(TEXT_INPUT_KEY);
                        SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> request.onConfirm().accept(text != null ? text.trim() : ""));
                    }, ClickCallback.Options.builder().uses(1).build())
            );

            ActionButton cancelButton = ActionButton.create(
                    cancelLabel,
                    null,
                    DEFAULT_BUTTON_WIDTH,
                    DialogAction.customClick((response, audience) -> SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> request.onCancel().run()), ClickCallback.Options.builder().uses(1).build())
            );

            builder.base(dialogBase);
            builder.type(DialogType.multiAction(
                    List.of(cancelButton, confirmButton),
                    null,
                    2
            ));
        });

        player.showDialog(dialog);
    }
}