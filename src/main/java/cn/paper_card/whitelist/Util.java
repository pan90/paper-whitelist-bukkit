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

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.UUID;

class Util {

    static void close(@NotNull InputStream inputStream, @NotNull InputStreamReader inputStreamReader, @NotNull BufferedReader reader) throws IOException {
        IOException exception = null;
        try {
            reader.close();
        } catch (IOException e) {
            exception = e;
        }

        try {
            inputStreamReader.close();
        } catch (IOException e) {
            exception = e;
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            exception = e;
        }

        if (exception != null) throw exception;
    }

    static void send(@NotNull HttpURLConnection connection, @NotNull Object data) throws IOException {
        final OutputStream out = connection.getOutputStream();

        final PrintStream stream = new PrintStream(out, false, StandardCharsets.UTF_8);

        stream.print(data);
        stream.flush();

        stream.close();
        out.close();
    }

    static @NotNull String readContent(@NotNull HttpURLConnection connection) throws IOException {
        final InputStream inputStream = connection.getInputStream();
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String line;

        final StringBuilder builder = new StringBuilder();

        try {
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
        } catch (IOException e) {
            try {
                close(inputStream, inputStreamReader, bufferedReader);
            } catch (IOException ignored) {
            }
            throw e;
        }

        close(inputStream, inputStreamReader, bufferedReader);

        return builder.toString();
    }

    static @NotNull JsonObject toJson(@NotNull WhitelistInfo info, @NotNull Server server) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", info.userId().toString());
        jsonObject.addProperty("remark", info.remark());
        jsonObject.addProperty("create_time", info.createTime());
        final String name = server.getOfflinePlayer(info.userId()).getName();
        jsonObject.addProperty("name", name);
        return jsonObject;
    }

    static @NotNull JsonObject toJson(@NotNull WhitelistCodeInfo info) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("code", info.code());
        jsonObject.addProperty("name", info.name());
        jsonObject.addProperty("uuid", info.uuid().toString());
        jsonObject.addProperty("create_time", info.createTime());
        jsonObject.addProperty("expires", info.expireTime());
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
        text.append(copyable(dateFormat().format(info.createTime() * 1000)));

        text.appendNewline();
        text.append(Component.text("备注："));
        text.append(copyable(info.remark()));
    }
}
