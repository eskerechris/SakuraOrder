package me.chris.sakuraOrder.api.services.order;

import me.chris.sakuraOrder.api.model.IOrder;
import me.chris.sakuraOrder.api.services.order.result.CreateResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Provides operations for validating, processing, and creating orders.
 */
public interface OrderCreateService {

    /**
     * Validates and creates an order.
     *
     * <p>The creation process may perform validation, apply domain rules,
     * process the required payment, and persist the order. The returned
     * result indicates whether the order was successfully created or why
     * the operation failed.</p>
     *
     * @param order the order to validate and create
     * @return a future completed with the result of the creation attempt
     */
    @NotNull
    CompletableFuture<CreateResult> createOrder(@NotNull IOrder order);
}