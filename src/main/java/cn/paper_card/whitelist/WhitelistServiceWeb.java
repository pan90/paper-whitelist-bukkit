package cn.paper_card.whitelist;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.client.api.Util;
import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import cn.paper_card.paper_whitelist.api.WhitelistService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

class WhitelistServiceWeb implements WhitelistService {


    private final @NotNull PluginMain plugin;

    private final @NotNull Gson gson = new Gson();

    WhitelistServiceWeb(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    private static @Nullable WhitelistInfo parse(@NotNull JsonObject jsonObject) {
        /*
         * {
         *     "uuid": "20554467-84cb-4773-a084-e3cfa867d480",
         *     "remark": "游戏名：Paper99，管理员Paper99在管理页面添加",
         *     "c_time": 1711688667
         * }
         */
        final JsonElement uuidEle = jsonObject.get("uuid");
        final JsonElement remarkEle = jsonObject.get("remark");
        final JsonElement createTimeEle = jsonObject.get("c_time");
        final JsonElement nameEle = jsonObject.get("name");

        if (uuidEle == null) return null;

        final UUID uuid = UUID.fromString(uuidEle.getAsString());

        return new WhitelistInfo(
                nameEle.getAsString(),
                uuid,
                remarkEle.getAsString(),
                createTimeEle != null ? createTimeEle.getAsLong() : -1
        );
    }


    @Override
    public void add(@NotNull WhitelistInfo whitelistInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@NotNull UUID userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable WhitelistInfo query(@NotNull UUID userId) throws IOException {

        final PaperClientApi api = plugin.getPaperClientApi();
        if (api == null) throw new IOException("PaperClientApi is null!");

        final URL url = new URL(api.getApiBase() + "/whitelist/" + userId);

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);

        final String jsonStr = Util.readData(connection);
        final JsonObject jsonObject = this.gson.fromJson(jsonStr, JsonObject.class);

        connection.disconnect();

        return parse(jsonObject);
    }
}
