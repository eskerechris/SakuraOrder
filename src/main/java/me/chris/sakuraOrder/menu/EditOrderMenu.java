package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.order.result.FinalizeResult;
import me.chris.sakuraOrder.menu.components.CancelOrderButton;
import me.chris.sakuraOrder.menu.components.CollectButton;
import me.chris.sakuraOrder.menu.components.OrderButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.menu.framework.TickableMenu;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EditOrderMenu extends Menu implements TickableMenu {

    private static final int[] BORDER_SLOTS = {0, 1, 2, 9, 11, 18, 19, 20};
    private static final int ORDER_DISPLAY_SLOT = 10;
    private static final int COLLECT_SLOT = 16;
    private static final int CANCEL_SLOT = 13;

    private final UUID orderId;
    private WrappedTask wrappedTask;

    public EditOrderMenu(@NotNull OrderPlugin plugin, @NotNull UUID orderId) {
        super(plugin);
        this.orderId = orderId;
        setSize(9 * 3);
        setTitle(plugin.getLangService().menuMessage("edit-order-menu.title"));
    }

    @Override
    protected void registerButtons() {
        ItemStack glassPane = buildGlassPane();

        for (int slot : BORDER_SLOTS) {
            addButton(Button.filler(slot, glassPane));
        }

        addButton(new OrderButton(ORDER_DISPLAY_SLOT, orderId, this, getPlugin()));
        addButton(new CollectButton(COLLECT_SLOT, orderId, getPlugin()));
        addButton(new CancelOrderButton(CANCEL_SLOT, orderId, this::handleFinalizeResult, getPlugin()));
    }

    @Override
    public void displayTo(@NotNull Player player) {
        super.displayTo(player);
        cancelTask();

        wrappedTask = SakuraOrder.getInstance().getSchedulerService().runAtEntityTimer(
                player,
                () -> tick(player.getServer().getCurrentTick()),
                20L,
                20L
        );
    }

    @Override
    public void tick(long currentTick) {
        updateDynamicButtons();
        updateInventory();
    }

    @Override
    public void onClose(@NotNull Player player) {
        super.onClose(player);
        cancelTask();
    }

    @NotNull
    private ItemStack buildGlassPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }

        return pane;
    }

    private void handleFinalizeResult(@NotNull FinalizeResult result) {
        Player viewer = getViewer();

        if (viewer == null) {
            return;
        }

        if (result instanceof FinalizeResult.Success success) {
            viewer.sendMessage(success.message());
            new OrderMenu(getPlugin()).displayTo(viewer);
            return;
        }

        updateInventory();
    }

    private void cancelTask() {
        if (wrappedTask != null) {
            wrappedTask.cancel();
            wrappedTask = null;
        }
    }
}
