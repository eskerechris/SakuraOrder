package me.chris.sakuraOrder.persistence;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Serializes persistence operations per order while allowing different orders
 * to be persisted concurrently.
 */
public class OrderPersistenceQueue {

    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

    /**
     * Enqueues a persistence operation for the given order.
     *
     * <p>Operations for the same order are executed sequentially, regardless
     * of whether previous operations completed successfully. Operations for
     * different orders remain independent.</p>
     *
     * @param orderId the order this operation belongs to
     * @param write supplies the persistence future once previous operations have completed
     * @return a future representing this persistence operation
     */
    @NotNull
    public CompletableFuture<Void> enqueue(
            @NotNull UUID orderId,
            @NotNull Supplier<CompletableFuture<Void>> write
    ) {
        CompletableFuture<Void> queued = tails.compute(orderId, (id, previousTail) -> {
            CompletableFuture<Void> previous = previousTail != null
                    ? previousTail
                    : CompletableFuture.completedFuture(null);

            return previous
                    .exceptionally(ex -> null)
                    .thenCompose(ignored -> write.get());
        });

        queued.whenComplete((v, ex) -> tails.remove(orderId, queued));
        return queued;
    }

    /**
     * Waits until all currently queued persistence operations have completed.
     *
     * <p>Failures do not prevent the queue from being considered flushed.</p>
     *
     * @return a future completed when no queued operation remains
     */
    @NotNull
    public CompletableFuture<Void> flush() {
        tails.entrySet().removeIf(entry -> entry.getValue().isDone());
        CompletableFuture<?>[] pending = tails.values().toArray(CompletableFuture[]::new);

        if (pending.length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(pending)
                .handle((ignored, ex) -> null)
                .thenCompose(ignored -> flush());
    }
}