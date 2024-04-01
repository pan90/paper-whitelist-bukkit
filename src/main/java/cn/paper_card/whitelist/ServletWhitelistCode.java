package cn.paper_card.whitelist;

import cn.paper_card.paper_whitelist.api.WhitelistCodeInfo;
import cn.paper_card.paper_whitelist.api.WhitelistCodeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

class ServletWhitelistCode extends HttpServlet {

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

    void sendResponse(@NotNull HttpServletResponse resp, @NotNull Response response) throws IOException {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json; charset=utf-8");

        final PrintWriter writer = resp.getWriter();
        writer.print(response.toJson());
        writer.close();
    }


    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PluginMain p = getPlugin();
        final Response response = this.doTakeCode(req.getParameter("code"), p);
        this.sendResponse(resp, response);
    }

    private @NotNull Response doTakeCode(@Nullable String argCode, @NotNull PluginMain p) {
        if (argCode == null) {
            return new Response(ErrorCode.MissingArgument, "必须提供参数：code");
        }

        final int code;

        try {
            code = Integer.parseInt(argCode);
        } catch (NumberFormatException e) {
            return new Response(ErrorCode.IllegalArgument, "不正确的code：" + argCode);
        }

        final WhitelistApiImpl api = p.getWhitelistApi();

        if (api == null) {
            return new Response(ErrorCode.ServiceUnavailable, "WhitelistApiImpl is null!");
        }

        final WhitelistCodeService service = api.getWhitelistCodeService();

        final WhitelistCodeInfo codeInfo;

        try {
            codeInfo = service.take(code);
        } catch (Exception e) {
            p.getSLF4JLogger().error("Fail to take code", e);
            return new Response(ErrorCode.ServiceUnavailable, "Fail to take code: " + e);
        }

        if (codeInfo == null) {
            return new Response(ErrorCode.NoWhitelistCode, "不存在的验证码：" + code);
        }

        return new Response(ErrorCode.Ok, "OK", Util.toJson(codeInfo));
    }
}
