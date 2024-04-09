package cn.paper_card.whitelist;

import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.UUID;

class OnPreLogin implements Listener {
    private final @NotNull PluginMain plugin;

    OnPreLogin(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    /*
//    void register() {
//        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
//    }
     */

    static void appendPlayerAndTime(@NotNull TextComponent.Builder text, @NotNull String name, @NotNull UUID uuid) {
        text.appendNewline();
        text.append(Component.text("游戏角色：%s (%s)".formatted(name, uuid)).color(NamedTextColor.GRAY));

        text.appendNewline();
        text.append(Component.text("时间：%s".formatted(Util.dateFormat().format(System.currentTimeMillis())))
                .color(NamedTextColor.GRAY));
    }

    static @NotNull TextComponent kickMessageWhenException(@NotNull Throwable e, @NotNull String name, @NotNull UUID uuid) {
        final TextComponent.Builder text = Component.text();
        text.append(Component.text("[ PaperCard | 系统错误 ]")
                .color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }

        appendPlayerAndTime(text, name, uuid);

        return text.build();
    }

    void kickWhenException(@NotNull AsyncPlayerPreLoginEvent event, @NotNull Throwable e) {
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        event.kickMessage(kickMessageWhenException(e, event.getName(), event.getUniqueId()));
    }

    static @NotNull TextComponent kickMessageNotWhitelist(@Nullable TextComponent suffix, @Nullable WhitelistCodeInfo info, @NotNull String name, @NotNull UUID uuid) {
        final TextComponent.Builder text = Component.text();

        text.append(Component.text("[ PaperCard | 白名单 ]")
                .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        text.appendNewline();
        text.append(Component.text("您尚未添加白名单，请先申请白名单").color(NamedTextColor.YELLOW));

        if (suffix != null) {
            text.appendNewline();
            text.append(suffix);
        }

        if (info != null) {

            text.appendNewline();
            text.append(Component.text("您的登录验证码：").color(NamedTextColor.GREEN));
            text.append(Component.text(info.code()).decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD));

            text.appendNewline();
            text.append(Component.text("请不要向其他人泄露您的验证码，否则您的身份可能会被盗用！")
                    .color(NamedTextColor.RED));

            text.appendNewline();
            text.append(Component.text("验证码有效时间："));
            text.append(Component.text(Util.minutesAndSeconds(
                            (info.expireTime() - info.createTime())
                    ))
                    .color(NamedTextColor.YELLOW));
            text.append(Component.text("内，被使用后立即失效"));

            text.appendNewline();
            text.append(Component.text("重连将生成新验证码，并且原验证码立即失效").color(NamedTextColor.YELLOW));
        }

        appendPlayerAndTime(text, name, uuid);

        return text.build();
    }

    void kickWhitelistCode(@NotNull AsyncPlayerPreLoginEvent event, @Nullable WhitelistCodeInfo info, @Nullable TextComponent suffix) {
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
        event.kickMessage(kickMessageNotWhitelist(suffix, info, event.getName(), event.getUniqueId()));
    }

    private void syncCache(@NotNull WhitelistApiImpl api, @Nullable WhitelistInfo info, @NotNull UUID uuid, @NotNull String name) {

        this.plugin.getTaskScheduler().runTaskAsynchronously(() -> {
            if (info != null) {
                try {
                    api.getLocalWhitelist().update(info);
                } catch (SQLException e) {
                    plugin.getSLF4JLogger().warn("", e);
                    return;
                }
                plugin.getSLF4JLogger().info("已更新本地白名单 {remark: %s}".formatted(info.remark()));
            } else {
                // 删除
                final boolean delete;

                try {
                    delete = api.getLocalWhitelist().delete(uuid);
                } catch (SQLException e) {
                    plugin.getSLF4JLogger().warn("", e);
                    return;
                }
                plugin.getSLF4JLogger().info("删除本地 %s 的本地白名单：%s".formatted(name, delete));
            }
        });

    }

    void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event, @Nullable TextComponent suffix) {
        final WhitelistApiImpl api = this.plugin.getWhitelistApi();
        if (api == null) {
            this.kickWhenException(event, new Exception("WhitelistApiImpl is null!"));
            return;
        }

        WhitelistInfo whitelistInfo = null;
        try {
            whitelistInfo = api.getWhitelistService().query(event.getUniqueId());

            // 更新缓存
            this.syncCache(api, whitelistInfo, event.getUniqueId(), event.getName());

        } catch (Exception e) {

            // 查询本地
            try {
                whitelistInfo = api.getLocalWhitelist().query(event.getUniqueId());
            } catch (SQLException ignored) {
                this.plugin.getSLF4JLogger().warn("无法查询本地白名单", e);
            }

            if (whitelistInfo == null) {
                final String msg = "无法查询白名单，请稍后重新连接";
                this.plugin.getSLF4JLogger().error(msg, e);
                this.kickWhenException(event, new Exception(msg, e));
                return;
            }
        }

        if (whitelistInfo != null) {
            // 白名单
            this.plugin.getSLF4JLogger().info("白名单：" + whitelistInfo.remark());
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            return;
        }

        // 非白名单
        // 添加标记
        event.getPlayerProfile().setProperty(new ProfileProperty("paper-not-whitelist", "true"));

        // 生成验证码
        if (plugin.getConfigManager().isGenerateCode()) {
            final WhitelistCodeInfo code;

            try {
                code = api.requestWhitelistCode(event.getUniqueId(), event.getName());
            } catch (Exception e) {
                final String msg = "生成白名单验证码失败，请稍后重试";
                this.plugin.getSLF4JLogger().error(msg, e);
                this.kickWhitelistCode(event, null, suffix);
                return;
            }
            this.kickWhitelistCode(event, code, suffix);
        } else {
            this.kickWhitelistCode(event, null, suffix);
        }
    }

    @EventHandler
    public void on(@NotNull AsyncPlayerPreLoginEvent event) {
        this.onPreLogin(event, null);
    }
}

