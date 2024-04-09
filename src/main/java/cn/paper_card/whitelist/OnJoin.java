package cn.paper_card.whitelist;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

class OnJoin implements Listener {

    private final @NotNull PluginMain plugin;

    OnJoin(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(@NotNull PlayerJoinEvent event) {
        // 再次检查白名单

        final Player player = event.getPlayer();

        final PlayerProfile profile = player.getPlayerProfile();
        final boolean hasProperty = profile.hasProperty("paper-not-whitelist");

        // 没有白名单则踢出
        if (hasProperty) {
            plugin.getTaskScheduler().runTaskLater(() -> {
                player.kick(OnPreLogin.kickMessageNotWhitelist(null, null, player.getName(), player.getUniqueId()));
            }, 1);
        }
    }
}
