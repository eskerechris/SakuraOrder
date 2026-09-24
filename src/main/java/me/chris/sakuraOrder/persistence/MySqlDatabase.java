package me.chris.sakuraOrder.persistence;

import me.chris.sakuraOrder.SakuraOrder;
import me.chris.sakuraOrder.api.services.config.SettingsService.DatabaseSettings;
import org.jetbrains.annotations.NotNull;

public class MySqlDatabase extends MySqlLikeDatabase {

    public MySqlDatabase(@NotNull SakuraOrder plugin, @NotNull DatabaseSettings settings) {
        super(plugin, settings);
    }

    @Override
    @NotNull
    protected String getJdbcScheme() {
        return "mysql";
    }
}