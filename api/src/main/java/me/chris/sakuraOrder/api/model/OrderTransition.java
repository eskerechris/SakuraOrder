package me.chris.sakuraOrder.api.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an atomic domain transition applied to a live order.
 *
 * <p>The transition is executed by the order cache as part of an atomic operation.
 * The resulting order is persisted and the cache is updated according to the returned
 * {@link Outcome}.</p>
 *
 * <p>Implementations of {@link #apply(IOrder)} must be fast and side-effect free.
 * They must not perform I/O or blocking operations, access the order cache, or mutate
 * the provided order. Orders follow a copy-on-write model, so a new order instance
 * should be returned when a state change is required.</p>
 *
 * <p>If {@link #apply(IOrder)} throws an exception, the current order remains unchanged
 * and the exception is propagated to the caller.</p>
 *
 * @param <R> the type of value returned to the caller alongside the resulting order
 */
@FunctionalInterface
public interface OrderTransition<R> {

    /**
     * Applies this transition to the current order.
     *
     * @param current the current live order
     * @return the transition outcome containing the resulting order and an optional value
     */
    @NotNull
    Outcome<R> apply(@NotNull IOrder current);

    /**
     * Represents the result of an order transition.
     *
     * <p>The returned order determines the state that should be persisted. Returning
     * the same instance that was provided to {@link #apply(IOrder)} indicates that
     * no state change occurred, so the order does not need to be persisted.</p>
     *
     * <p>Returning a different order instance indicates that the state has changed.
     * The new state is persisted through the ordered persistence queue. If the resulting
     * order is no longer live, it is removed from the cache and the appropriate close
     * listeners are notified.</p>
     *
     * @param <R> the type of value returned to the caller
     * @param order the resulting order state
     * @param value the optional value to return to the caller
     */
    record Outcome<R>(@NotNull IOrder order, @Nullable R value) {

        /**
         * Creates an outcome indicating that the order was not modified.
         *
         * @param current the unchanged order instance
         * @param value the optional value to return to the caller
         * @param <R> the type of the returned value
         * @return an unchanged transition outcome
         */
        public static <R> Outcome<R> unchanged(@NotNull IOrder current, @Nullable R value) {
            return new Outcome<>(current, value);
        }

        /**
         * Creates an outcome indicating that the order was modified.
         *
         * @param next the resulting order state
         * @param value the optional value to return to the caller
         * @param <R> the type of the returned value
         * @return a changed transition outcome
         */
        public static <R> Outcome<R> changed(@NotNull IOrder next, @Nullable R value) {
            return new Outcome<>(next, value);
        }
    }
}