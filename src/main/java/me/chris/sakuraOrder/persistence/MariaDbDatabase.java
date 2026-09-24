package me.chris.sakuraOrder.persistence;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.services.config.SettingsService.DatabaseSettings;
import org.jetbrains.annotations.NotNull;

public class MariaDbDatabase extends MySqlLikeDatabase {

    public MariaDbDatabase(@NotNull SakuraOrder plugin, @NotNull DatabaseSettings settings) {
        super(plugin, settings);
    }

    @Override
    @NotNull
    protected String getJdbcScheme() {
        return "mariadb";
    }
}