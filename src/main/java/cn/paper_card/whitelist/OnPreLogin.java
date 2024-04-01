package cn.paper_card.whitelist;

import cn.paper_card.paper_whitelist.api.WhitelistCodeInfo;
import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    void appendPlayerAndTime(@NotNull TextComponent.Builder text, @NotNull String name, @NotNull UUID uuid) {
        text.appendNewline();
        text.append(Component.text("游戏角色：%s (%s)".formatted(name, uuid)).color(NamedTextColor.GRAY));

        text.appendNewline();
        text.append(Component.text("时间：%s".formatted(Util.dateFormat().format(System.currentTimeMillis())))
                .color(NamedTextColor.GRAY));
    }

    void kickWhenException(@NotNull AsyncPlayerPreLoginEvent event, @NotNull Throwable e) {

        final TextComponent.Builder text = Component.text();
        text.append(Component.text("[ PaperCard | 系统错误 ]")
                .color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }

        this.appendPlayerAndTime(text, event.getName(), event.getUniqueId());

        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        event.kickMessage(text.build());
    }

    void kickWhitelistCode(@NotNull AsyncPlayerPreLoginEvent event, @NotNull WhitelistCodeInfo info, @Nullable TextComponent suffix) {
        final TextComponent.Builder text = Component.text();

        text.append(Component.text("[ PaperCard | 白名单 ]")
                .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        text.appendNewline();
        text.append(Component.text("您尚未添加白名单，请先申请白名单").color(NamedTextColor.YELLOW));

        text.appendNewline();
        text.append(Component.text("您的验证码：").color(NamedTextColor.GREEN));
        text.append(Component.text(info.code()).decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD));

        if (suffix != null) {
            text.appendNewline();
            text.append(suffix);
        }

        text.appendNewline();
        text.append(Component.text("请不要向其他人泄露您的验证码，否则您的身份可能会被盗用！")
                .color(NamedTextColor.RED));

        text.appendNewline();
        text.append(Component.text("验证码有效时间："));
        text.append(Component.text(Util.minutesAndSeconds(
                        (info.expires() - info.createTime())
                ))
                .color(NamedTextColor.YELLOW));
        text.append(Component.text("内，被使用后立即失效"));

        text.appendNewline();
        text.append(Component.text("重连将生成新验证码，并且原验证码立即失效").color(NamedTextColor.YELLOW));

        this.appendPlayerAndTime(text, event.getName(), event.getUniqueId());

        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
        event.kickMessage(text.build().color(NamedTextColor.GREEN));
    }

    void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event, @Nullable TextComponent suffix) {
        final WhitelistApiImpl api = this.plugin.getWhitelistApi();
        if (api == null) {
            this.kickWhenException(event, new Exception("WhitelistApiImpl is null!"));
            return;
        }

        final WhitelistInfo whitelistInfo;

        try {
            whitelistInfo = api.getWhitelistService().query(event.getUniqueId());
        } catch (Exception e) {
            final String msg = "无法查询白名单！";
            this.plugin.getSLF4JLogger().error(msg, e);
            this.kickWhenException(event, new Exception(msg, e));
            return;
        }

        if (whitelistInfo != null) {
            // 白名单
            this.plugin.getSLF4JLogger().info("白名单：" + whitelistInfo.remark());
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            return;
        }

        // 非白名单

        // 生成验证码
        final WhitelistCodeInfo code;

        try {
            code = api.getWhitelistCodeService().create(event.getUniqueId(), event.getName());
        } catch (Exception e) {
            final String msg = "生成白名单验证码失败！";
            this.plugin.getSLF4JLogger().error(msg, e);
            this.kickWhenException(event, new Exception(msg, e));
            return;
        }

        this.kickWhitelistCode(event, code, suffix);
    }

    @EventHandler
    public void on(@NotNull AsyncPlayerPreLoginEvent event) {
        this.onPreLogin(event, null);
    }
}

