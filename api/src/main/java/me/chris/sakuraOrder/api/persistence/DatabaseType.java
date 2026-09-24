package me.chris.sakuraOrder.api.persistence;

import org.jetbrains.annotations.NotNull;

/**
 * Defines the database types supported by SakuraOrder.
 */
public enum DatabaseType {

    /**
     * SQLite database.
     */
    SQLITE,

    /**
     * MySQL database.
     */
    MYSQL,

    /**
     * MariaDB database.
     */
    MARIADB;

    /**
     * Converts a string into its corresponding database type.
     *
     * <p>If the provided name does not match a supported database type,
     * {@link #SQLITE} is returned as the default.</p>
     *
     * @param name the database type name
     * @return the corresponding database type, or {@link #SQLITE} if the name is invalid
     */
    @NotNull
    public static DatabaseType fromString(@NotNull String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SQLITE;
        }
    }
}