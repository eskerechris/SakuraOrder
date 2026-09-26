package me.chris.sakuraOrder;

import me.chris.sakuraOrder.api.OrderPlugin;
import me.chris.sakuraOrder.api.services.dialog.DialogService;
import me.chris.sakuraOrder.api.services.economy.EconomyService;
import me.chris.sakuraOrder.api.services.material.MaterialBlacklistService;
import me.chris.sakuraOrder.api.services.sound.SoundService;
import me.chris.sakuraOrder.menu.framework.Menu;
import me.chris.sakuraOrder.menu.framework.MenuListener;
import me.chris.sakuraOrder.api.persistence.OrderRepository;
import me.chris.sakuraOrder.api.services.config.SettingsService;
import me.chris.sakuraOrder.api.services.gui.GuiService;
import me.chris.sakuraOrder.api.services.lang.LangService;
import me.chris.sakuraOrder.command.CommandManager;
import me.chris.sakuraOrder.services.config.PluginSettingsService;
import me.chris.sakuraOrder.persistence.Database;
import me.chris.sakuraOrder.persistence.SqlOrderRepository;
import me.chris.sakuraOrder.services.dialog.OrderDialogServiceFactory;
import me.chris.sakuraOrder.services.economy.VaultEconomyService;
import me.chris.sakuraOrder.services.lang.PluginLangService;
import me.chris.sakuraOrder.services.gui.PluginGuiService;
import me.chris.sakuraOrder.services.material.BlacklistService;
import me.chris.sakuraOrder.services.order.*;
import me.chris.sakuraOrder.services.order.*;
import me.chris.sakuraOrder.services.scheduler.SchedulerService;
import me.chris.sakuraOrder.services.sound.PluginSoundService;
import me.chris.sakuraOrder.util.StartupBanner;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.chris.sakuraOrder.api.services.order.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main JavaPlugin implementation entry point for SakuraOrder.
 */
public final class SakuraOrder extends JavaPlugin implements OrderPlugin {

    public static SakuraOrder instance;

    private static final int ARCHIVE_BATCH_SIZE = 500;
    private static final long ARCHIVE_INTERVAL_TICKS = 20L * 60L * 5L;

    private SettingsService settingsService;
    private LangService langService;
    private SchedulerService schedulerService;
    private EconomyService economyService;
    private FoliaLib foliaLib;
    private Database database;
    private OrderRepository orderRepository;
    private OrderCacheService orderCacheService;
    private GuiService guiService;
    private MaterialBlacklistService materialBlacklistService;
    private OrderCreateService orderCreateService;
    private OrderDeliveryService orderDeliveryService;
    private OrderCancelService orderCancelService;
    private OrderCollectService orderCollectService;
    private OrderRefundService orderRefundService;
    private OrderExpirationService orderExpirationService;
    private OrderHistoryService orderHistoryService;
    private DialogService dialogService;
    private SoundService soundService;

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<Integer>> activeArchiveTask = new AtomicReference<>(CompletableFuture.completedFuture(0));
    private WrappedTask archiveCompletedOrdersTaskHandle;

    @Override
    public void onEnable() {
        instance = this;

        // bstats
        final int pluginId = 34197;
        new Metrics(this, pluginId);

        shuttingDown.set(false);

        saveDefaultConfig();

        // Initialize folialib
        foliaLib = new FoliaLib(this);

        // Initialize settings service first
        this.settingsService = new PluginSettingsService(this);

        // Initialize language service after settings (to read the configured locale)
        this.langService = new PluginLangService(this);

        // Initialize platform-appropriate scheduler service (Bukkit vs Folia)
        this.schedulerService = new SchedulerService(foliaLib);

        // Initialize economy service
        this.economyService = new VaultEconomyService(this);

        // Load database settings and let Database.create() pick the concrete
        // dialect (SqliteDatabase/MySqlDatabase/MariaDbDatabase) accordingly
        this.database = Database.create(this, settingsService.getDatabase());
        this.database.initialize();
        this.orderRepository = new SqlOrderRepository(this, database);

        // Initialize services
        this.orderCacheService = new CacheService(this);
        this.orderRefundService = new RefundService(this);
        this.orderExpirationService = new ExpirationService(this, orderCacheService, orderRefundService);
        this.orderCacheService.loadActiveOrders().thenRun(orderExpirationService::start);
        this.guiService = new PluginGuiService(this);
        this.materialBlacklistService = new BlacklistService(this);
        this.orderCreateService = new CreateService(this);
        this.orderDeliveryService = new DeliveryService(this);
        this.orderCancelService = new CancelService(this, orderCacheService, orderRefundService);
        this.orderCollectService = new CollectService(this);
        this.orderHistoryService = new HistoryService(this, orderCacheService);
        this.orderHistoryService.start();
        this.dialogService = OrderDialogServiceFactory.create(this);
        this.soundService = new PluginSoundService(this, schedulerService);

        // Archive terminal orders task
        archiveCompletedOrdersTaskHandle = this.schedulerService.runTimerAsync(
                this::archiveTerminalOrders,
                ARCHIVE_INTERVAL_TICKS,
                ARCHIVE_INTERVAL_TICKS
        );

        // Register GUI inventory listener
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // Register modern Paper Brigadier commands
        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> new CommandManager(this).registerCommands(event.registrar())
        );

        // Print plugin banner
        StartupBanner.print(this, foliaLib);
    }

    @Override
    public void onDisable() {
        shuttingDown.set(true);

        // Stop order expiration check task
        if (orderExpirationService != null) {
            orderExpirationService.stop();
        }

        // Cancel the scheduled task for archiving completed orders
        if (archiveCompletedOrdersTaskHandle != null && !archiveCompletedOrdersTaskHandle.isCancelled()) {
            archiveCompletedOrdersTaskHandle.cancel();
        }

        this.orderHistoryService.stop();

        waitForActiveArchiveTask();

        // Close open GUI menus safely (triggers MenuListener.onInventoryClose synchronously)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }

        // Wait for queued state transitions (including COMPLETED after claim grace) before
        // moving terminal rows to history and closing the connection pool.
        if (orderCacheService != null) {
            try {
                orderCacheService.flush().join();
            } catch (CompletionException ex) {
                getLogger().warning("Some pending order writes did not complete during shutdown");
            }
        }

        archiveTerminalOrdersBeforeShutdown();

        // Close the HikariCP pool safely
        if (database != null) {
            database.shutdown();
        }

        // Unregister all event listeners cleanly
        HandlerList.unregisterAll(this);

        // Cancel all task cleanly
        schedulerService.cancelAllTasks();
    }

    private void archiveTerminalOrdersBeforeShutdown() {
        if (orderRepository == null) return;

        try {
            int archived;
            do {
                archived = orderRepository.archiveCompletedOrders(ARCHIVE_BATCH_SIZE).join();
            } while (archived == ARCHIVE_BATCH_SIZE);
        } catch (CompletionException ex) {
            getLogger().warning("Failed to archive terminal orders during shutdown");
        }
    }

    private void archiveTerminalOrders() {
        if (shuttingDown.get()) {
            return;
        }

        CompletableFuture<Integer> previous = activeArchiveTask.get();
        if (!previous.isDone()) {
            return; // a batch is already running
        }

        CompletableFuture<Integer> taskFuture = new CompletableFuture<>();
        if (!activeArchiveTask.compareAndSet(previous, taskFuture)) {
            return; // lost the race to another invocation; that one owns this cycle
        }

        try {
            orderRepository.archiveCompletedOrders(ARCHIVE_BATCH_SIZE)
                    .whenComplete((archived, ex) -> {
                        if (ex == null) {
                            taskFuture.complete(archived);
                        } else {
                            taskFuture.completeExceptionally(ex);
                            getLogger().warning("Failed to archive terminal orders");
                        }
                    });
        } catch (RuntimeException ex) {
            taskFuture.completeExceptionally(ex);
            getLogger().warning("Failed to start terminal-order archival");
        }
    }

    private void waitForActiveArchiveTask() {
        try {
            activeArchiveTask.get().join();
        } catch (CompletionException ex) {
            getLogger().warning("The active terminal-order archival did not complete cleanly during shutdown");
        }
    }

    @Override
    public void reload() {
        // Does not currently reload database settings/pool at runtime;
        // a config change to the database section still requires a full restart.
        settingsService.reload();
        materialBlacklistService.reload();
        soundService.reload();
        langService.reload();
    }

    @Override
    public @NotNull SettingsService getSettingsService() {
        return settingsService;
    }

    @Override
    public @NotNull LangService getLangService() {
        return langService;
    }

    public @NotNull SchedulerService getSchedulerService() {
        return schedulerService;
    }

    @Override
    public @NotNull EconomyService getEconomyService() {
        return economyService;
    }

    @Override
    public @NonNull OrderCacheService getOrderCacheService() {
        return orderCacheService;
    }

    @Override
    public @NotNull GuiService getGuiService() {
        return guiService;
    }

    @Override
    public @NotNull MaterialBlacklistService getMaterialBlacklistService() {
        return materialBlacklistService;
    }

    @Override
    public @NotNull OrderRepository getOrderRepository() {
        return orderRepository;
    }

    @Override
    public @NotNull OrderCreateService getOrderCreateService() {
        return orderCreateService;
    }

    @Override
    public @NotNull OrderDeliveryService getOrderDeliveryService() {
        return orderDeliveryService;
    }

    @Override
    public @NotNull OrderCancelService getOrderCancelService() {
        return orderCancelService;
    }

    @Override
    public @NotNull OrderCollectService getOrderCollectService() {
        return orderCollectService;
    }

    @Override
    public @NotNull OrderHistoryService getOrderHistoryService() {
        return orderHistoryService;
    }

    @Override
    public @NotNull DialogService getDialogService() {
        return dialogService;
    }

    @Override
    public @NotNull SoundService getSoundService() {
        return soundService;
    }

    public static SakuraOrder getInstance() {
        return instance;
    }
}