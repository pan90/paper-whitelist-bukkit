package cn.paper_card.whitelist;

import cn.paper_card.paper_whitelist.api.WhitelistCodeInfo;
import cn.paper_card.paper_whitelist.api.WhitelistCodeService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

class WhitelistCodeServiceWeb implements WhitelistCodeService {

    private final @NotNull PluginMain plugin;

    private final @NotNull Gson gson;

    WhitelistCodeServiceWeb(@NotNull PluginMain plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    private @NotNull WhitelistCodeInfo parse(@NotNull JsonObject json) {
        /*
         * {
         *     "code": 52704,
         *     "uuid": "20554467-84cb-4773-a084-e3cfa867d480",
         *     "name": "Paper99",
         *     "c_time": 1711692142,
         *     "expires": 1711692442
         * }
         */

        final JsonElement codeEle = json.get("code");
        final JsonElement uuidEle = json.get("uuid");
        final JsonElement nameEle = json.get("name");
        final JsonElement createTimeEle = json.get("c_time");
        final JsonElement expiresEle = json.get("expires");

        return new WhitelistCodeInfo(
                codeEle.getAsInt(),
                UUID.fromString(uuidEle.getAsString()),
                nameEle.getAsString(),
                createTimeEle.getAsLong(),
                expiresEle.getAsLong()
        );
    }

    @Override
    public @NotNull WhitelistCodeInfo create(@NotNull UUID id, @NotNull String name) throws IOException {
        final HttpURLConnection connection = getHttpURLConnection();

        // 发送数据
        final JsonObject json = new JsonObject();
        json.addProperty("uuid", id.toString());
        json.addProperty("name", name);
        Util.send(connection, json.toString());

        final String jsonStr = Util.readContent(connection);
        connection.disconnect();

        final JsonObject jsonObject = this.gson.fromJson(jsonStr, JsonObject.class);

        return this.parse(jsonObject);
    }

    @NotNull
    private HttpURLConnection getHttpURLConnection() throws IOException {
        final URL url = new URL(this.plugin.getConfigManager().getApiBase() + "/whitelist-code");

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("paper-token", this.plugin.getConfigManager().getPaperToken());

        connection.connect();
        return connection;
    }

    @Override
    public @Nullable WhitelistCodeInfo take(int code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable WhitelistCodeInfo query(@NotNull UUID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable WhitelistCodeInfo take(@NotNull UUID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int deleteExpires() {
        throw new UnsupportedOperationException();
    }
}
