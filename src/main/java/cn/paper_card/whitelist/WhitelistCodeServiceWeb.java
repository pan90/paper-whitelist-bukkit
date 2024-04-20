package cn.paper_card.whitelist;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.client.api.PaperResponseError;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

class WhitelistCodeServiceWeb {

    private final @NotNull PluginMain plugin;

    WhitelistCodeServiceWeb(@NotNull PluginMain plugin) {
        this.plugin = plugin;
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
        final JsonElement expiresEle = json.get("e_time");

        return new WhitelistCodeInfo(
                codeEle.getAsInt(),
                UUID.fromString(uuidEle.getAsString()),
                nameEle.getAsString(),
                createTimeEle.getAsLong(),
                expiresEle.getAsLong()
        );
    }

    public @NotNull WhitelistCodeInfo create(@NotNull UUID id, @NotNull String name) throws IOException, PaperResponseError {
        final PaperClientApi api = plugin.getPaperClientApi();
        if (api == null) throw new IOException("PaperClientApi is null!");

        // 请求参数
        final JsonObject params = new JsonObject();
        params.addProperty("uuid", id.toString());
        params.addProperty("name", name);


        final JsonElement dataEle;
        dataEle = api.sendRequest("/whitelist-code", params, "POST");

        if (dataEle == null) throw new IOException("dataEle is null!");

        final JsonObject dataObj = dataEle.getAsJsonObject();

        return parse(dataObj);
    }
}
