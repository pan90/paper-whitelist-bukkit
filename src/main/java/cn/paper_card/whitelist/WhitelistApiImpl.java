package cn.paper_card.whitelist;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.sql.SQLException;

class WhitelistApiImpl implements PaperWhitelistApi {

    private final @NotNull WhitelistServiceImpl whitelistService;
    private final @NotNull WhitelistCodeServiceImpl whitelistCodeService;

    private final @NotNull PluginMain plugin;

    WhitelistApiImpl(@NotNull DatabaseApi.MySqlConnection connection, @NotNull PluginMain plugin) {
        this.whitelistService = new WhitelistServiceImpl(connection);
        this.whitelistCodeService = new WhitelistCodeServiceImpl(connection);
        this.plugin = plugin;
    }

    @Override
    public @NotNull WhitelistServiceImpl getWhitelistService() {
        return this.whitelistService;
    }

    @Override
    public @NotNull WhitelistCodeServiceImpl getWhitelistCodeService() {
        return this.whitelistCodeService;
    }

    void destroy() {
        final Logger l = this.plugin.getSLF4JLogger();
        try {
            this.whitelistService.destroy();
        } catch (SQLException e) {
            l.error("", e);
        }

        try {
            this.whitelistCodeService.destroy();
        } catch (SQLException e) {
            l.error("", e);
        }
    }
}