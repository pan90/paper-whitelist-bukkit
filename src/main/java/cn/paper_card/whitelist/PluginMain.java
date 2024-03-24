package cn.paper_card.whitelist;

import cn.paper_card.MojangProfileApi;
import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 1 查询自己的白名单信息，/whitelist
// todo: 2 /whitelist list [页码] 指令
// 3 /whitelist code 生成自己的白名单验证码

public final class PluginMain extends JavaPlugin {

    private WhitelistApiImpl whitelistApi = null;
    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull MojangProfileApi mojangProfileApi;

    public PluginMain() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
        this.mojangProfileApi = new MojangProfileApi();
    }

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

        new MainCommand(this).register(this);
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
}
