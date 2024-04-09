package cn.paper_card.whitelist;

import cn.paper_card.MojangProfileApi;
import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

public final class PluginMain extends JavaPlugin {

    private WhitelistApiImpl whitelistApi = null;
    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull MojangProfileApi mojangProfileApi;

    private final @NotNull ConfigManager configManager;

    public PluginMain() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.mojangProfileApi = new MojangProfileApi();
        this.configManager = new ConfigManager(this);
    }

    void registerApi() throws SQLException {
        if (this.whitelistApi != null) return;

        final DatabaseApi api = this.getServer().getServicesManager().load(DatabaseApi.class);
        if (api == null) throw new RuntimeException("未连接到DatabaseApi！");

        this.whitelistApi = new WhitelistApiImpl(this, api.getLocalSQLite().connectImportant());
        this.getServer().getServicesManager().register(PaperWhitelistApi.class, this.whitelistApi, this, ServicePriority.Highest);
    }

    @Override
    public void onLoad() {
        try {
            this.registerApi();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        try {
            this.registerApi();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        new MainCommand(this).register(this);

        this.configManager.getAll();
        this.configManager.save();

        final String apiBase = this.configManager.getApiBase();
        this.getSLF4JLogger().info("api base: " + apiBase);

        // 注册事件监听
        final PluginManager pm = this.getServer().getPluginManager();
        final Plugin p = pm.getPlugin("PaperPreLogin");
        if (p == null) {
            pm.registerEvents(new Listener() {
                @EventHandler
                public void on(@NotNull AsyncPlayerPreLoginEvent event) {
                    final WhitelistApiImpl api = getWhitelistApi();
                    if (api == null) return;
                    api.onPreLogin(event, null);
                }
            }, this);
        }

        pm.registerEvents(new OnJoin(this), this);
    }

    @Override
    public void onDisable() {
        final WhitelistApiImpl api = this.whitelistApi;
        this.whitelistApi = null;

        this.getServer().getServicesManager().unregisterAll(this);

        if (api != null) api.destroy();

        this.taskScheduler.cancelTasks(this);
    }

    @Nullable WhitelistApiImpl getWhitelistApi() {
        return this.whitelistApi;
    }

    @NotNull MojangProfileApi getMojangProfileApi() {
        return this.mojangProfileApi;
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull ConfigManager getConfigManager() {
        return this.configManager;
    }
}
