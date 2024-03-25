package cn.paper_card.whitelist;

import cn.paper_card.mc_command.NewMcCommand;
import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

// /whitelist list [页码] 指令
class CommandList extends NewMcCommand {
    private final @NotNull Permission permission;

    private final @NotNull MainCommand parentCmd;

    protected CommandList(@NotNull MainCommand parentCmd) {
        super("list");
        this.parentCmd = parentCmd;
        this.permission = this.addSubPermission(
                parentCmd.getPlugin().getServer().getPluginManager(),
                parentCmd.getPermission()
        );
    }

    @Override
    protected void appendPrefix(TextComponent.@NotNull Builder text) {
        this.parentCmd.appendPrefix(text);
    }

    @Override
    protected boolean canExecute(@NotNull CommandSender commandSender) {
        return commandSender.hasPermission(this.permission);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        final PluginMain p = this.parentCmd.getPlugin();

        p.getTaskScheduler().runTaskAsynchronously(() -> {
            final Sender ms = new Sender(sender);

            final String argPageNo = args.length > 0 ? args[0] : null;

            final int pageNo;
            if (argPageNo == null) {
                pageNo = 1;
            } else {
                if (args.length != 1) {
                    ms.error("只需要1个参数，提供了%d个参数！".formatted(args.length));
                    return;
                }

                try {
                    pageNo = Integer.parseInt(argPageNo);
                } catch (NumberFormatException e) {
                    ms.error("不正确的页码：%s".formatted(argPageNo));
                    return;
                }

                if (pageNo <= 0) {
                    ms.error("不正确的页码：%s".formatted(argPageNo));
                    return;
                }
            }

            final int pageSize = 1;

            final WhitelistApiImpl api = p.getWhitelistApi();
            if (api == null) {
                ms.error("WhitelistApiImpl is null!");
                return;
            }

            final List<WhitelistInfo> list;

            try {
                list = api.getWhitelistService().queryPage(pageSize, (pageNo - 1) * pageSize);
            } catch (SQLException e) {
                p.getSLF4JLogger().error("Fail to query whitelist by page", e);
                ms.error("查询白名单失败！");
                ms.exception(e);
                return;
            }

            final TextComponent.Builder text = Component.text();
            this.appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("==== 白名单信息 | 第%d页 ===="));

            final int size = list.size();
            if (size == 0) {
                text.appendNewline();
                text.append(Component.text("本页没有任何内容啦").color(NamedTextColor.GRAY));
            } else {
                for (WhitelistInfo info : list) {
                    Util.appendInfo(text, info, p.getServer().getOfflinePlayer(info.userId()).getName());
                }
            }

            text.appendNewline();

            final boolean hasPre = pageNo > 1;
            final String preCmd = "/%s %s %d".formatted(
                    this.parentCmd.getLabel(),
                    this.getLabel(),
                    pageNo - 1
            );
            text.append(Component.text("[上一页]")
                    .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                    .clickEvent(hasPre ? ClickEvent.runCommand(preCmd) : null)
                    .hoverEvent(HoverEvent.showText(Component.text(hasPre ? "点击上一页" : "没有上一页啦")))
            );
            text.appendSpace();

            final boolean noNext = size < pageSize;
            final String nextCmd = "/%s %s %d".formatted(
                    this.parentCmd.getLabel(),
                    this.getLabel(),
                    pageNo + 1
            );
            text.append(Component.text("[下一页]")
                    .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                    .clickEvent(noNext ? null : ClickEvent.runCommand(nextCmd))
                    .hoverEvent(HoverEvent.showText(Component.text(noNext ? "没有下一页啦" : "点击下一页")))
            );

            sender.sendMessage(text.build().color(NamedTextColor.GREEN));
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1) {
            final String arg = args[0];
            if (arg.isEmpty()) return Collections.singletonList("[页码]");
            return null;
        }

        return null;
    }
}
