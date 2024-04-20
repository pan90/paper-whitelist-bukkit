package cn.paper_card.whitelist;

import cn.paper_card.client.api.PaperResponseError;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

class WhitelistApiImpl implements PaperWhitelistApi {

    private final @NotNull WhitelistServiceWeb whitelistService;

    private final @NotNull WhitelistCodeServiceWeb whitelistCodeService;

    private final @NotNull OnPreLogin onPreLogin;

    private final @NotNull LocalWhitelist localWhitelist;

    private final @NotNull PluginMain plugin;

    WhitelistApiImpl(@NotNull PluginMain plugin, @NotNull Connection connection) {
        this.plugin = plugin;
        this.whitelistService = new WhitelistServiceWeb(plugin);
        this.onPreLogin = new OnPreLogin(plugin);
        this.whitelistCodeService = new WhitelistCodeServiceWeb(plugin);
        this.localWhitelist = new LocalWhitelist(connection);
    }

    @Override
    public @NotNull WhitelistServiceWeb getWhitelistService() {
        return this.whitelistService;
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

    @NotNull WhitelistCodeInfo requestWhitelistCode(@NotNull UUID uuid, @NotNull String name) throws IOException, PaperResponseError {
        return this.whitelistCodeService.create(uuid, name);
    }

    @NotNull LocalWhitelist getLocalWhitelist() {
        return this.localWhitelist;
    }

    void destroy() {

        try {
            this.localWhitelist.destroy();
        } catch (SQLException e) {
            this.plugin.getSLF4JLogger().error("", e);
        }
    }
}