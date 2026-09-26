package me.chris.sakuraOrder.menu.components;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.SakuraSound;
import me.chris.sakuraOrder.api.model.TextInputRequest;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.menu.framework.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SearchButton extends Button {

    private final OrderPlugin plugin;
    private final LangService lang;
    private final Consumer<String> onSearch;

    public SearchButton(OrderPlugin plugin, int slot, Consumer<String> onSearch) {
        super(slot);
        this.plugin = plugin;
        this.lang = plugin.getLangService();
        this.onSearch = onSearch;
    }

    private void openDialog(Player player) {
        plugin.getDialogService().openTextInput(player, new TextInputRequest(
                lang.menuMessage("paginated-menu.search-button.dialog-title"),
                lang.menuMessage("paginated-menu.search-button.dialog-prompt"),
                "",
                50,
                new ItemStack(Material.OAK_SIGN),
                query -> SakuraOrder.getInstance().getSchedulerService().runAtEntity(
                        player,
                        () -> onSearch.accept(query)
                ),
                () -> {
                }
        ));
    }

    @Override
    protected @NotNull ItemStack createItem() {
        var item = new ItemStack(Material.OAK_SIGN);
        var meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(lang.menuMessage("paginated-menu.search-button.name"));
        meta.lore(lang.menuMessageList("paginated-menu.search-button.lore"));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
        openDialog(player);
        plugin.getSoundService().play(player, SakuraSound.BUTTON_INTERACT);
    }
}