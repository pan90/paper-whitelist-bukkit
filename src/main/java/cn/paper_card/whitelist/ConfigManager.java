package cn.paper_card.whitelist;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

class ConfigManager {

    private final @NotNull PluginMain plugin;

    ConfigManager(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    boolean isGenerateCode() {
        final FileConfiguration c = this.plugin.getConfig();

        @NotNull String path_generate_code = "generate-code";
        if (!c.contains(path_generate_code, true)) {
            c.set(path_generate_code, false);

            final LinkedList<String> comments = new LinkedList<>();
            comments.add(" ");
            comments.add("是否生成白名单验证码");
            comments.add("配置为true时，无白名单的玩家登录服务器会生成一个白名单验证码");
            comments.add("玩家可以使用这个验证码登录网站申请白名单");

            c.setComments(path_generate_code, comments);
        }

        return c.getBoolean(path_generate_code);
    }

    void getAll() {
        this.isGenerateCode();
    }

    void save() {
        this.plugin.saveConfig();
    }

    void reload() {
        this.plugin.reloadConfig();
    }

}
