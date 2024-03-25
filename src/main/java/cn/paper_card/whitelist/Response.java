package cn.paper_card.whitelist;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


record Response(
        @NotNull ErrorCode errorCode,
        @NotNull String errorMessage,
        @Nullable JsonObject data
) {

    Response(@NotNull ErrorCode errorCode, @NotNull String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    @NotNull JsonObject toJson() {
        final JsonObject json = new JsonObject();
        json.addProperty("ec", this.errorCode.ordinal());
        json.addProperty("em", this.errorMessage);

        if (this.data != null) json.add("data", this.data);

        return json;
    }
}