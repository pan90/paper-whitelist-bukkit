package cn.paper_card.whitelist;

import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.UUID;

class Util {

    static @NotNull JsonObject toJson(@NotNull WhitelistInfo info, @NotNull Server server) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", info.userId().toString());
        jsonObject.addProperty("remark", info.remark());
        jsonObject.addProperty("create_time", info.createTime());
        final String name = server.getOfflinePlayer(info.userId()).getName();
        jsonObject.addProperty("name", name);
        return jsonObject;
    }

    static @NotNull String minutesAndSeconds(long seconds) {
        final long minutes = seconds / 60;
        seconds %= 60;

        final StringBuilder sb = new StringBuilder();
        if (minutes != 0) {
            sb.append(minutes);
            sb.append("分");
        }

        if (seconds != 0 || minutes == 0) {
            sb.append(seconds);
            sb.append("秒");
        }

        return sb.toString();
    }

    static @NotNull TextComponent copyable(@NotNull String text) {
        return Component.text(text).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(text))
                .hoverEvent(HoverEvent.showText(Component.text("点击复制").color(NamedTextColor.GREEN)));
    }

    static @NotNull TextComponent link(@NotNull String link) {
        return Component.text(link).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(link))
                .hoverEvent(HoverEvent.showText(Component.text("点击打开").color(NamedTextColor.GREEN)));
    }

    static @NotNull SimpleDateFormat dateFormat() {
        return new SimpleDateFormat("yyyy年MM月dd日_HH:mm:ss");
    }

    static void appendInfo(@NotNull TextComponent.Builder text, @NotNull WhitelistInfo info, @Nullable String name) {

        final UUID id = info.userId();
        final String idStr = id.toString();

        if (name != null) {
            text.appendNewline();
            text.append(Component.text("游戏名："));
            text.append(copyable(name));
        }


        text.appendNewline();
        text.append(Component.text("UUID："));
        text.append(copyable(idStr));

        text.appendNewline();
        text.append(Component.text("详细信息："));
        text.append(link("https://minecraftuuid.com/?search=" + idStr));

        text.appendNewline();
        text.append(Component.text("添加时间："));
        text.append(copyable(dateFormat().format(info.createTime())));

        text.appendNewline();
        text.append(Component.text("备注："));
        text.append(copyable(info.remark()));
    }
}
