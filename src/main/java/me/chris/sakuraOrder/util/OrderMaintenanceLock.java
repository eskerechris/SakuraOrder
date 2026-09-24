package me.chris.sakuraOrder.util;

/**
 * Provides a global, thread-safe lock used to temporarily prevent operations
 * from being executed.
 * <p>
 * Order's services will check whether the lock is active and skip their operations
 */
public final class OrderMaintenanceLock {

    private OrderMaintenanceLock() {}

    private static volatile boolean locked = false;

    public static boolean isLocked() {
        return locked;
    }

    public static void lock() {
        locked = true;
    }

    public static void unlock() {
        locked = false;
    }
}