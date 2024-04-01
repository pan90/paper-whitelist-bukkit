package cn.paper_card.whitelist;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

class ConfigManager {

    private final @NotNull String path_paper_token = "paper-token";

    private final @NotNull String path_api_base = "api-base";

    private final @NotNull PluginMain plugin;

    ConfigManager(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    @NotNull String getPaperToken() {
        final FileConfiguration c = this.plugin.getConfig();

        if (!c.contains(path_paper_token, true)) {
            c.setInlineComments(path_paper_token, Collections.singletonList("# paper token"));
            c.set(path_paper_token, "paper-card");
        }

        return Objects.requireNonNull(c.getString(path_paper_token));
    }

    @NotNull String getApiBase() {
        final FileConfiguration c = this.plugin.getConfig();

        if (!c.contains(path_api_base, true)) {
            c.setInlineComments(path_api_base, Collections.singletonList("# api base"));
            c.set(path_api_base, "https://paper-card.cn/api");
        }

        return Objects.requireNonNull(c.getString(path_api_base));
    }

    void getAll() {
        this.getApiBase();
        this.getPaperToken();
    }

    void save() {
        this.plugin.saveConfig();
    }

    void reload() {
        this.plugin.reloadConfig();
    }

}
