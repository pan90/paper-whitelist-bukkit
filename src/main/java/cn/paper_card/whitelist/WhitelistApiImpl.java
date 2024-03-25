package cn.paper_card.whitelist;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
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

    private final @NotNull OnPreLogin onPreLogin;

    WhitelistApiImpl(@NotNull DatabaseApi.MySqlConnection connection, @NotNull PluginMain plugin) {
        this.whitelistService = new WhitelistServiceImpl(connection);
        this.whitelistCodeService = new WhitelistCodeServiceImpl(connection);
        this.plugin = plugin;
        this.onPreLogin = new OnPreLogin(plugin);
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

    void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event, @Nullable TextComponent suffix) {
        this.onPreLogin.onPreLogin(event, suffix);
    }

    @Override
    public void onPreLoginCheck(@NotNull Object event, @Nullable Object suffix) {
        if (suffix instanceof TextComponent tc) {
            this.onPreLogin((AsyncPlayerPreLoginEvent) event, tc);
        } else {
            this.onPreLogin((AsyncPlayerPreLoginEvent) event, null);
        }
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