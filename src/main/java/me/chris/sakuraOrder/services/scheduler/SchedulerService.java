package me.chris.sakuraOrder.services.scheduler;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Thin facade over FoliaLib's {@link PlatformScheduler}
 */
public final class SchedulerService {

    private final PlatformScheduler scheduler;
    private final boolean folia;

    public SchedulerService(@NotNull FoliaLib foliaLib) {
        this.scheduler = foliaLib.getScheduler();
        this.folia = foliaLib.isFolia();
    }

    /** Runs a task on the global scheduler at the next tick. */
    public void runNextTick(@NotNull Runnable task) {
        scheduler.runNextTick(t -> task.run());
    }

    /** Runs a task on the global scheduler now if already on the global thread, otherwise next tick. */
    public void runNowOrNextTick(@NotNull Runnable task) {
        boolean onThread = folia ? Bukkit.isGlobalTickThread() : Bukkit.isPrimaryThread();
        if (onThread) {
            task.run();
        } else {
            runNextTick(task);
        }
    }

    /** Repeating task on the global scheduler. */
    @NotNull
    public WrappedTask runTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTimer(task, clamp(delayTicks), clamp(periodTicks));
    }

    /**
     * Repeating asynchronous task. Must not directly access thread-confined
     * game state.
     */
    @NotNull
    public WrappedTask runTimerAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTimerAsync(task, clamp(delayTicks), clamp(periodTicks));
    }

    /** Runs a task on the scheduler owning the entity's region at the next tick. */
    public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        scheduler.runAtEntity(entity, t -> task.run());
    }

    /** Runs a task now if the caller already owns the entity's region, otherwise via {@link #runAtEntity}. */
    public void runNowOrAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        boolean onThread = folia ? Bukkit.isOwnedByCurrentRegion(entity) : Bukkit.isPrimaryThread();
        if (onThread) {
            task.run();
        } else {
            runAtEntity(entity, task);
        }
    }

    /** Runs a task on the entity's scheduler after a delay (minimum 1 tick). */
    public void runAtEntityLater(@NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
        scheduler.runAtEntityLater(entity, task, clamp(delayTicks));
    }

    /** Repeating task on the scheduler owning the entity's region. */
    @NotNull
    public WrappedTask runAtEntityTimer(@NotNull Entity entity, @NotNull Runnable task,
                                        long delayTicks, long periodTicks) {
        return scheduler.runAtEntityTimer(entity, task, clamp(delayTicks), clamp(periodTicks));
    }

    /** Cancels every task scheduled through FoliaLib. */
    public void cancelAllTasks() {
        scheduler.cancelAllTasks();
    }

    private static long clamp(long ticks) {
        return Math.max(1L, ticks);
    }
}