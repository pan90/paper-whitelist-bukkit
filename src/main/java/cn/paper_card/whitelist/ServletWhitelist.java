package cn.paper_card.whitelist;

import cn.paper_card.MojangProfileApi;
import cn.paper_card.mc_command.NewMcCommand;
import cn.paper_card.paper_whitelist.api.AlreadyWhitelistedException;
import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

class ServletWhitelist extends HttpServlet {

    private PluginMain plugin = null;

    @Override
    public void init() throws ServletException {
        super.init();
        final Object o = this.getServletContext().getAttribute("plugin");
        if (o instanceof final PluginMain p) {
            this.plugin = p;
        } else {
            throw new ServletException("缺失Attribute：plugin");
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        super.service(req, resp);
    }

    @NotNull PluginMain getPlugin() throws ServletException {
        if (this.plugin == null) throw new ServletException("plugin is null!");
        return this.plugin;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PluginMain p = this.getPlugin();

        final String uuid = req.getParameter("uuid");

        final Response response;
        if (uuid != null) {
            response = this.doQueryOne(p, uuid);
        } else {
            response = this.doQueryPage(req.getParameter("page"),
                    req.getParameter("size"),
                    req.getParameter("search"),
                    p
            );
        }
        this.sendResponse(resp, response);
    }

    void sendResponse(@NotNull HttpServletResponse resp, @NotNull Response response) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        final PrintWriter writer = resp.getWriter();
        writer.print(response.toJson());
        writer.close();
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PluginMain p = this.getPlugin();

        final String uuid = req.getParameter("uuid");

        final Response response = this.doRemoveWhitelist(p, uuid);

        this.sendResponse(resp, response);
    }

    private @Nullable JsonObject parseJson(@NotNull BufferedReader reader) throws IOException {
        final Gson gson = new Gson();
        final JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
        reader.close();
        return jsonObject;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PluginMain p = this.getPlugin();

        final JsonObject json = this.parseJson(req.getReader());

        final Response response = this.doAddWhitelist(json, p);

        this.sendResponse(resp, response);
    }

    private @NotNull Response doQueryPage(@Nullable String argPage, @Nullable String argSize, @Nullable String argSearch, @NotNull PluginMain p) {
        if (argPage == null) {
            return new Response(ErrorCode.MissingArgument, "必须提供参数：page（页码）");
        }

        if (argSize == null) {
            return new Response(ErrorCode.MissingArgument, "必须提供参数：size（每页数量）");
        }

        final int page;
        final int size;

        try {
            page = Integer.parseInt(argPage);

            if (page <= 0) return new Response(ErrorCode.IllegalArgument, "page应该是正整数！");
        } catch (NumberFormatException e) {
            return new Response(ErrorCode.IllegalArgument, "不正确的page参数：" + argPage);
        }

        try {
            size = Integer.parseInt(argSize);

            if (size <= 0) return new Response(ErrorCode.IllegalArgument, "size应该是正整数！");
        } catch (NumberFormatException e) {
            return new Response(ErrorCode.IllegalArgument, "不正确的size参数：" + argSize);
        }

        final WhitelistApiImpl api = p.getWhitelistApi();
        if (api == null) {
            return new Response(ErrorCode.ServiceUnavailable, "WhitelistApiImpl is null!");
        }

        final WhitelistServiceImpl service = api.getWhitelistService();

        final int count;

        try {
            count = service.queryCount();
        } catch (SQLException e) {
            p.getSLF4JLogger().error("Fail to query whitelist count", e);
            return new Response(ErrorCode.ServiceUnavailable, "Fail to query whitelist count: " + e);
        }

        if (argSearch == null) {

            final List<WhitelistInfo> list;

            try {
                list = service.queryPage(size, (page - 1) * size);
            } catch (SQLException e) {
                p.getSLF4JLogger().error("Fail to query whitelist", e);
                return new Response(ErrorCode.ServiceUnavailable, "Fail to query whitelist: " + e);
            }

            final JsonArray array = new JsonArray();
            for (WhitelistInfo info : list) {
                final JsonObject json = Util.toJson(info, p.getServer());
                array.add(json);
            }

            final JsonObject jsonObject = new JsonObject();
            jsonObject.add("list", array);
            jsonObject.addProperty("total", count);
            return new Response(ErrorCode.Ok, "OK", jsonObject);
        } else {
            // todo
            return new Response(ErrorCode.ServiceUnavailable, "TODO");
        }
    }

    private @NotNull Response doQueryOne(@NotNull PluginMain p, @NotNull String uuidStr) {
        final UUID uuid;

        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return new Response(ErrorCode.IllegalArgument, "非法的UUID：" + uuidStr);
        }

        final WhitelistApiImpl api = p.getWhitelistApi();
        if (api == null) {
            return new Response(ErrorCode.ServiceUnavailable, "WhitelistApiImpl is null!");
        }

        final WhitelistInfo info;

        try {
            info = api.getWhitelistService().query(uuid);
        } catch (SQLException e) {
            p.getSLF4JLogger().error("Fail to query whitelist", e);
            return new Response(ErrorCode.ServiceUnavailable, "Fail to query whitelist: " + e);
        }

        if (info == null) {
            return new Response(ErrorCode.NotWhitelist, "未添加白名单");
        }

        return new Response(ErrorCode.Ok, "OK", Util.toJson(info, p.getServer()));
    }

    private @NotNull Response doRemoveWhitelist(@NotNull PluginMain p, @Nullable String uuidStr) {
        if (uuidStr == null) {
            return new Response(ErrorCode.MissingArgument, "必须提供参数：uuid");
        }

        final UUID uuid;

        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return new Response(ErrorCode.IllegalArgument, "非法的UUID：" + uuidStr);
        }

        final WhitelistApiImpl api = p.getWhitelistApi();

        if (api == null) {
            return new Response(ErrorCode.ServiceUnavailable, "WhitelistApiImpl is null!");
        }

        final WhitelistServiceImpl ser = api.getWhitelistService();

        final WhitelistInfo info;

        try {
            info = ser.query(uuid);
        } catch (SQLException e) {
            p.getSLF4JLogger().error("Fail to query whitelist", e);
            return new Response(ErrorCode.ServiceUnavailable, "Fail to query whitelist: " + e);
        }

        if (info == null) {
            return new Response(ErrorCode.NotWhitelist, "无需删除，没有添加白名单");
        }

        final boolean remove;

        try {
            remove = ser.remove(uuid);
        } catch (SQLException e) {
            p.getSLF4JLogger().error("Fail to remove whitelist", e);
            return new Response(ErrorCode.ServiceUnavailable, "Fail to remove whitelist: " + e);
        }

        if (!remove) {
            return new Response(ErrorCode.Ok, "没有数据被删除", Util.toJson(info, p.getServer()));
        } else {
            return new Response(ErrorCode.Ok, "已删除白名单", Util.toJson(info, p.getServer()));
        }
    }

    private @NotNull Response doAddWhitelist(@Nullable JsonObject json, @NotNull PluginMain p) {
        if (json == null) {
            return new Response(ErrorCode.EmptyBody, "请求体为空");
        }

        final JsonElement nameEle = json.get("name");
        String name = nameEle != null ? nameEle.getAsString() : null;

        final JsonElement uuidEle = json.get("uuid");
        String uuidStr = uuidEle != null ? uuidEle.getAsString() : null;

        final JsonElement remarkEle = json.get("remark");
        String remark = remarkEle != null ? remarkEle.getAsString() : null;

        UUID uuid = null;

        if (uuidStr == null) {
            if (name != null) {

                final OfflinePlayer offlinePlayer = NewMcCommand.parseOfflinePlayerName(name, p.getServer());
                if (offlinePlayer != null) {
                    uuid = offlinePlayer.getUniqueId();
                }

                if (uuid == null) {
                    final MojangProfileApi.Profile profile;

                    try {
                        profile = p.getMojangProfileApi().requestByName(name);
                    } catch (Exception e) {
                        p.getSLF4JLogger().warn("Fail to request mojang profile", e);
                        return new Response(ErrorCode.ServiceUnavailable, "Fail to request mojang profile");
                    }

                    uuid = profile.uuid();
                }

            } else {
                return new Response(ErrorCode.MissingArgument, "uuid和name至少提供一个");
            }
        } else {
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                return new Response(ErrorCode.IllegalArgument, "非法UUID：" + uuidStr);
            }
        }


        final WhitelistApiImpl api = p.getWhitelistApi();
        if (api == null) {
            return new Response(ErrorCode.ServiceUnavailable, "WhitelistApiImpl is null!");
        }

        final WhitelistServiceImpl service = api.getWhitelistService();

        if (remark == null) {

            if (name == null) {
                name = p.getServer().getOfflinePlayer(uuid).getName();
            }

            if (name == null) {
                try {
                    final MojangProfileApi.Profile profile = p.getMojangProfileApi().requestByUuid(uuid);
                    name = profile.name();
                } catch (Exception e) {
                    p.getSLF4JLogger().warn("Fail to request mojang profile", e);
                }
            }

            remark = "游戏名：%s，通过WebAPI添加".formatted(name);
        }

        final WhitelistInfo info = new WhitelistInfo(
                uuid,
                remark,
                System.currentTimeMillis()
        );

        try {
            service.add(info);
        } catch (SQLException e) {
            p.getSLF4JLogger().error("Fail to add whitelist", e);
            return new Response(ErrorCode.ServiceUnavailable, e.toString());
        } catch (AlreadyWhitelistedException e) {
            return new Response(ErrorCode.AlreadyWhitelisted, "已经是白名单，无需重复添加", Util.toJson(e.getWhitelistInfo(), p.getServer()));
        }

        return new Response(ErrorCode.Ok, "OK", Util.toJson(info, p.getServer()));
    }
}
