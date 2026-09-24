package me.chris.sakuraOrder.menu;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.model.OrderableItem;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.menu.components.NavigationButton;
import me.chris.sakuraOrder.menu.framework.Button;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.util.OrderableItemCatalog;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;

public class EnchantSelectMenu extends Menu {

    private final LangService lang;
    private final Material baseMaterial;
    private final BiConsumer<Player, OrderableItem> onSelect;

    private static final int PREVIEW_SLOT = 0;
    private static final int PREVIEW_GLASS_1 = 1;
    private static final int PREVIEW_GLASS_2 = 9;
    private static final int PREVIEW_GLASS_3 = 10;

    private static final int[] FIXED_SLOTS = {18, 27, 36, 19};

    private static final int[] GRID_SLOTS = {
            3, 4, 5, 6,
            12, 13, 14, 15,
            21, 22, 23, 24,
            30, 31, 32, 33,
            39, 40, 41
    };

    private static final int CANCEL_SLOT = 46;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int CONFIRM_SLOT = 52;

    private final List<EnchantEntry> availableEntries;
    private final Map<Enchantment, Integer> selected = new HashMap<>();
    private int currentPage = 0;

    public EnchantSelectMenu(@NotNull OrderPlugin plugin, @NotNull Material baseMaterial,
                             @NotNull BiConsumer<Player, OrderableItem> onSelect) {
        super(plugin);
        this.lang = plugin.getLangService();
        this.baseMaterial = baseMaterial;
        this.onSelect = onSelect;
        this.availableEntries = buildAvailableEntries(baseMaterial);

        setTitle(lang.menuMessage("enchant-select-menu.title"));
        setSize(9 * 6);
    }

    private static List<EnchantEntry> buildAvailableEntries(@NotNull Material baseMaterial) {
        ItemStack probe = new ItemStack(baseMaterial);
        List<EnchantEntry> entries = new ArrayList<>();

        for (Enchantment enchantment : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
            if (enchantment.equals(Enchantment.UNBREAKING) || enchantment.equals(Enchantment.MENDING)) continue;
            if (!enchantment.canEnchantItem(probe)) continue;

            for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                entries.add(new EnchantEntry(enchantment, level));
            }
        }

        entries.sort(Comparator
                .comparing((EnchantEntry e) -> e.enchantment().getKey().getKey())
                .thenComparingInt(EnchantEntry::level));

        return entries;
    }

    /**
     * A material can only be selected in this menu if it accepts at least one
     * enchantment other than Unbreaking or Mending. Pre-built catalogue variants,
     * such as potions and enchanted books, are excluded as they do not require
     * additional enchantment selection.
     */
    public static boolean isEnchantable(@NotNull Material material) {
        if (material == Material.ENCHANTED_BOOK || material == Material.POTION
                || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION
                || material == Material.TIPPED_ARROW) {
            return false;
        }

        ItemStack probe = new ItemStack(material);
        for (Enchantment enchantment : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
            if (enchantment.canEnchantItem(probe)) {
                return true;
            }
        }
        return false;
    }

    private int getItemsPerPage() {
        return GRID_SLOTS.length;
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil(availableEntries.size() / (double) getItemsPerPage()));
    }

    @Override
    protected void registerButtons() {
        int totalPages = getTotalPages();
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        addButton(Button.filler(PREVIEW_GLASS_1, blackGlass()));
        addButton(Button.filler(PREVIEW_GLASS_2, blackGlass()));
        addButton(Button.filler(PREVIEW_GLASS_3, blackGlass()));
        addButton(Button.filler(PREVIEW_SLOT, buildPreviewItem()));

        registerFixedEnchant(FIXED_SLOTS[0], Enchantment.UNBREAKING, 1);
        registerFixedEnchant(FIXED_SLOTS[1], Enchantment.UNBREAKING, 2);
        registerFixedEnchant(FIXED_SLOTS[2], Enchantment.UNBREAKING, 3);
        registerFixedEnchant(FIXED_SLOTS[3], Enchantment.MENDING, 1);

        int from = currentPage * getItemsPerPage();
        int to = Math.min(from + getItemsPerPage(), availableEntries.size());
        for (int i = from; i < to; i++) {
            addButton(buildEntryButton(GRID_SLOTS[i - from], availableEntries.get(i)));
        }

        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;

        addButton(Button.of(CANCEL_SLOT, buildCancelItem(), (player, click) -> onCancel(player)));
        addButton(Button.of(CONFIRM_SLOT, buildConfirmItem(), (player, click) -> onConfirm(player)));
        addButton(new NavigationButton(getPlugin(), PREV_SLOT, false, hasPrev, () -> changePage(currentPage - 1)));
        addButton(new NavigationButton(getPlugin(), NEXT_SLOT, true, hasNext, () -> changePage(currentPage + 1)));
    }

    private void changePage(int newPage) {
        this.currentPage = newPage;
        refresh();
    }

    private void refresh() {
        clearButtons();
        registerButtons();

        for (int slot : GRID_SLOTS) {
            getInventory().setItem(slot, null);
        }
        updateInventory();
    }

    private void registerFixedEnchant(int slot, @NotNull Enchantment enchantment, int level) {
        addButton(buildEntryButton(slot, new EnchantEntry(enchantment, level)));
    }

    private Button buildEntryButton(int slot, @NotNull EnchantEntry entry) {
        boolean isSelected = Objects.equals(selected.get(entry.enchantment()), entry.level());
        boolean isConflicting = isConflicting(entry.enchantment());

        return Button.of(slot, buildEntryItem(entry, isSelected, isConflicting), (player, click) -> {
            if (isConflicting) return;

            Integer currentLevel = selected.get(entry.enchantment());
            if (currentLevel != null && currentLevel == entry.level()) {
                selected.remove(entry.enchantment());
            } else {
                selected.put(entry.enchantment(), entry.level());
            }
            refresh();
        });
    }

    private boolean isConflicting(@NotNull Enchantment enchantment) {
        if (selected.containsKey(enchantment)) return false;

        for (Enchantment other : selected.keySet()) {
            if (enchantment.conflictsWith(other)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack buildEntryItem(@NotNull EnchantEntry entry, boolean isSelected, boolean isConflicting) {
        ItemStack item = new ItemStack(isConflicting ? Material.GRAY_DYE : Material.ENCHANTED_BOOK);
        var meta = item.getItemMeta();
        if (meta == null) return item;

        String enchantName = OrderableItemCatalog.formatEnumName(entry.enchantment().getKey().getKey())
                + " " + OrderableItemCatalog.toRoman(entry.level());

        meta.displayName(lang.menuMessage(isSelected
                        ? "enchant-select-menu.entry.selected-name"
                        : "enchant-select-menu.entry.name",
                Map.of("enchant", enchantName)));

        List<Component> lore = new ArrayList<>();
        if (isConflicting) {
            lore.add(lang.menuMessage("enchant-select-menu.entry.not-applicable"));
        } else {
            lore.addAll(lang.menuMessageList(isSelected
                    ? "enchant-select-menu.entry.lore-selected"
                    : "enchant-select-menu.entry.lore"));
        }
        meta.lore(lore);

        if (isSelected) {
            meta.addEnchant(entry.enchantment(), entry.level(), true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPreviewItem() {
        ItemStack item = new ItemStack(baseMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        for (Map.Entry<Enchantment, Integer> entry : selected.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCancelItem() {
        var item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(lang.menuMessage("enchant-select-menu.cancel-button.name"));
        meta.lore(lang.menuMessageList("enchant-select-menu.cancel-button.lore"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmItem() {
        var item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(lang.menuMessage("enchant-select-menu.confirm-button.name"));
        meta.lore(lang.menuMessageList("enchant-select-menu.confirm-button.lore"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack blackGlass() {
        var item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private void onCancel(@NotNull Player player) {
        new MaterialSelectMenu(getPlugin(), onSelect).displayTo(player);
    }

    private void onConfirm(@NotNull Player player) {
        ItemStack finalItem = buildPreviewItem();
        String displayName = OrderableItemCatalog.formatEnumName(baseMaterial.name());
        onSelect.accept(player, new OrderableItem(finalItem, baseMaterial, displayName));
    }

    private record EnchantEntry(@NotNull Enchantment enchantment, int level) {}
}