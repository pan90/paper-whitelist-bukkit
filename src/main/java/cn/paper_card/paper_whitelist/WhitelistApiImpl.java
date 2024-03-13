package cn.paper_card.paper_whitelist;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.function.Supplier;

class WhitelistApiImpl implements PaperWhitelistApi {

    private final @NotNull WhitelistServiceImpl whitelistService;
    private final @NotNull WhitelistCodeServiceImpl whitelistCodeService;

    private final @NotNull Supplier<Logger> logger;

    WhitelistApiImpl(@NotNull DatabaseApi.MySqlConnection connection, @NotNull Supplier<Logger> logger) {
        this.whitelistService = new WhitelistServiceImpl(connection);
        this.whitelistCodeService = new WhitelistCodeServiceImpl(connection);
        this.logger = logger;
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
        final Logger l = this.logger.get();
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
