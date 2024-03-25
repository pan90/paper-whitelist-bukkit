package cn.paper_card.whitelist;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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


    @Override
    public @Nullable Object getServletContextHandler() {
        final ServletContextHandler handler = new ServletContextHandler();
        handler.setAttribute("plugin", this.plugin);
        handler.setContextPath("/api");
        handler.addServlet(new ServletHolder(new ServletWhitelist()), "/whitelist");
        handler.addServlet(new ServletHolder(new ServletWhitelistCode()), "/whitelist-code");
        return handler;
    }

    @Override
    public void onPreLoginCheck(@NotNull Object event, @Nullable Object suffix) {
        // todo
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