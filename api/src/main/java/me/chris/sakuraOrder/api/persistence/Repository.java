package me.chris.sakuraOrder.api.persistence;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Defines an asynchronous persistence contract for a domain entity.
 *
 * @param <T> the entity type
 * @param <ID> the entity identifier type
 */
public interface Repository<T, ID> {

    /**
     * Asynchronously saves or updates an entity.
     *
     * <p>If an entity with the same identifier already exists, its persisted
     * state is updated; otherwise, a new record is created.</p>
     *
     * @param entity the entity to save
     * @return a future that completes when the operation finishes
     */
    @NotNull
    CompletableFuture<Void> save(@NotNull T entity);

    /**
     * Asynchronously retrieves an entity by its identifier.
     *
     * @param id the identifier of the entity to retrieve
     * @return a future containing the entity if it exists, or an empty
     *         {@link Optional} otherwise
     */
    @NotNull
    CompletableFuture<Optional<T>> findById(@NotNull ID id);

    /**
     * Asynchronously deletes an entity by its identifier.
     *
     * @param id the identifier of the entity to delete
     * @return a future containing {@code true} if an entity was deleted,
     *         or {@code false} if no matching entity existed
     */
    @NotNull
    CompletableFuture<Boolean> deleteById(@NotNull ID id);
}