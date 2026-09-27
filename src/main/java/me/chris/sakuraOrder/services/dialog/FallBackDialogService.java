package me.chris.sakuraOrder.services.dialog;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.TextInputRequest;
import me.chris.sakuraOrder.api.services.dialog.DialogService;
import me.chris.sakuraOrder.menu.framework.Menu;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback {@link DialogService} for servers without the Paper Dialog API,
 * capturing the player's next chat message as input.
 */
public class FallBackDialogService implements DialogService, Listener {

    private final OrderPlugin plugin;
    private final Map<UUID, TextInputRequest> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> reopenTarget = new ConcurrentHashMap<>();
    private final Set<UUID> chainStarted = ConcurrentHashMap.newKeySet();

    public FallBackDialogService(@NotNull OrderPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void openTextInput(@NotNull Player player, @NotNull TextInputRequest request) {
        UUID uuid = player.getUniqueId();
        pending.put(uuid, request);

        if (chainStarted.add(uuid)) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getType() != InventoryType.CRAFTING) {
                reopenTarget.put(uuid, top);
                if (top.getHolder() instanceof Menu menu) {
                    menu.suspend();
                }
            }
        }

        player.closeInventory();
        player.sendMessage(plugin.getLangService().message("dialog.hint", Map.of(
                "prompt", PlainTextComponentSerializer.plainText().serialize(request.prompt()),
                "cancel", cancelKeyword()
        )));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        TextInputRequest request = pending.get(uuid);
        if (request == null) return;

        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (input.equalsIgnoreCase(cancelKeyword())) {
            pending.remove(uuid);
            SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> {
                request.onCancel().run();
                finishChain(player);
            });
            return;
        }

        if (request.maxLength() > 0 && input.length() > request.maxLength()) {
            player.sendMessage(plugin.getLangService().message("dialog.too-long", Map.of(
                    "max", String.valueOf(request.maxLength())
            )));
            return;
        }

        pending.remove(uuid);
        SakuraOrder.getInstance().getSchedulerService().runAtEntity(player, () -> {
            request.onConfirm().accept(input);
            if (!pending.containsKey(uuid)) {
                finishChain(player);
            }
        });
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        TextInputRequest request = pending.remove(uuid);
        chainStarted.remove(uuid);
        reopenTarget.remove(uuid);
        if (request != null) {
            SakuraOrder.getInstance().getSchedulerService().runAtEntity(event.getPlayer(), request.onCancel());
        }
    }

    private void finishChain(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        chainStarted.remove(uuid);
        Inventory captured = reopenTarget.remove(uuid);
        if (captured == null) {
            return;
        }

        if (captured.getHolder() instanceof Menu menu) {
            menu.resume();
            if (menu.getInventory() != captured) {
                return;
            }
        }

        player.openInventory(captured);
    }

    private @NonNull String cancelKeyword() {
        return PlainTextComponentSerializer.plainText()
                .serialize(plugin.getLangService().message("dialog.cancel-keyword"));
    }
}