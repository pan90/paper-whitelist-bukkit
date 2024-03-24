package cn.paper_card.whitelist;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginMain extends JavaPlugin {

    private WhitelistApiImpl whitelistApi = null;

    void registerApi() {
        if (this.whitelistApi != null) return;

        final DatabaseApi api = this.getServer().getServicesManager().load(DatabaseApi.class);
        if (api == null) throw new RuntimeException("未连接到DatabaseApi！");

        final DatabaseApi.MySqlConnection connection = api.getRemoteMySQL().getConnectionImportant();

        this.whitelistApi = new WhitelistApiImpl(connection, this);
        this.getServer().getServicesManager().register(PaperWhitelistApi.class, this.whitelistApi, this, ServicePriority.Highest);
    }

    @Override
    public void onLoad() {
        this.registerApi();
    }

    @Override
    public void onEnable() {
        this.registerApi();
    }

    @Override
    public void onDisable() {
        final WhitelistApiImpl api = this.whitelistApi;
        this.whitelistApi = null;
        this.getServer().getServicesManager().unregisterAll(this);

        if (api != null) api.destroy();
    }
}
