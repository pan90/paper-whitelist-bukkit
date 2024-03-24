package cn.paper_card.whitelist;

import cn.paper_card.MojangProfileApi;
import cn.paper_card.mc_command.NewMcCommand;
import cn.paper_card.paper_whitelist.api.AlreadyWhitelistedException;
import cn.paper_card.paper_whitelist.api.WhitelistCodeInfo;
import cn.paper_card.paper_whitelist.api.WhitelistInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

class MainCommand extends NewMcCommand.HasSub {

    private final @NotNull PluginMain plugin;

    private final @NotNull Permission permission;

    public MainCommand(@NotNull PluginMain plugin) {
        super("whitelist");
        this.plugin = plugin;
        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("paper-whitelist.command"));

        this.addSub(new AddRemove(true));
        this.addSub(new AddRemove(false));
        this.addSub(new Get());
        this.addSub(new Code());
    }

    @Override
    protected boolean canExecute(@NotNull CommandSender commandSender) {
        return commandSender.hasPermission(this.permission);
    }

    @Override
    protected void appendPrefix(TextComponent.@NotNull Builder builder) {
        builder.append(Component.text("[").color(NamedTextColor.GRAY));
        builder.append(Component.text("白名单").color(NamedTextColor.DARK_AQUA));
        builder.append(Component.text("]").color(NamedTextColor.GRAY));
    }

    @Override
    protected boolean onThisCommand(@NotNull CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return super.onThisCommand(sender);
        }

        // todo: 未测试

        // 查询自己的白名单

        plugin.getTaskScheduler().runTaskAsynchronously(() -> {
            final Sender sd = new Sender(sender);

            final WhitelistApiImpl api = plugin.getWhitelistApi();
            if (api == null) {
                sd.error("WhitelistApiImpl is null!");
                return;
            }

            final WhitelistInfo info;
            try {
                info = api.getWhitelistService().query(player.getUniqueId());
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("Fail to query whitelist", e);
                sd.error("查询白名单失败！");
                sd.exception(e);
                return;
            }

            if (info == null) {
                sd.info("您未添加白名单");
                return;
            }

            final TextComponent.Builder text = Component.text();
            this.appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("==== 您的白名单信息 ===="));
            Util.appendInfo(text, info, player.getName());
            sender.sendMessage(text.build().color(NamedTextColor.GREEN));

        });


        return true;
    }

    private class AddRemove extends NewMcCommand {

        private final @NotNull Permission permission;

        private final boolean isAdd;

        protected AddRemove(boolean isAdd) {
            super(isAdd ? "add" : "remove");
            this.permission = this.addSubPermission(plugin.getServer().getPluginManager(), MainCommand.this.permission);
            this.isAdd = isAdd;
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            MainCommand.this.appendPrefix(text);
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return commandSender.hasPermission(this.permission);
        }

        private void doAdd(@NotNull Sender sd,
                           @NotNull MojangProfileApi.Profile profile,
                           @NotNull CommandSender sender,
                           @NotNull String argPlayer,
                           @NotNull WhitelistApiImpl api
        ) {
            final var info = new WhitelistInfo(profile.uuid(),
                    "游戏名：%s，管理员%s使用指令添加".formatted(
                            profile.name(),
                            sender.getName()
                    ),
                    System.currentTimeMillis()
            );

            try {
                api.getWhitelistService().add(info);
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("Fail to add whitelist", e);
                sd.error("添加白名单失败！");
                sd.exception(e);
                return;
            } catch (AlreadyWhitelistedException e) {
                sd.warning("该玩家 %s 已添加白名单，无需重复添加".formatted(argPlayer));
                return;
            }

            final TextComponent.Builder text = Component.text();
            this.appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("添加白名单成功 :D"));
            Util.appendInfo(text, info, profile.name());

            sender.sendMessage(text.build().color(NamedTextColor.GREEN));
        }

        // 删除白名单
        private void doRemove(@NotNull WhitelistApiImpl api, @NotNull MojangProfileApi.Profile profile, @NotNull Sender sd, @NotNull CommandSender sender) {
            // 先查询
            final WhitelistInfo info;

            try {
                info = api.getWhitelistService().query(profile.uuid());
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("Fail to query whitelist", e);
                sd.error("查询白名单失败！");
                sd.exception(e);
                return;
            }

            if (info == null) {
                sd.warning("玩家 %s 没有白名单".formatted(profile.name()));
                return;
            }

            // 删除

            final boolean remove;
            try {
                remove = api.getWhitelistService().remove(profile.uuid());
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("Fail to remove whitelist", e);
                sd.error("删除白名单失败！");
                sd.exception(e);
                return;
            }

            if (!remove) {
                sd.error("没有任何数据被删除！");
                return;
            }

            final TextComponent.Builder text = Component.text();
            this.appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("删除白名单成功 :D"));
            Util.appendInfo(text, info, profile.name());

            sender.sendMessage(text.build().color(NamedTextColor.GREEN));
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

            final String argPlayer = args.length > 0 ? args[0] : null;

            final Sender sd = new Sender(sender);

            if (argPlayer == null) {
                sd.error("必须提供参数：玩家名或UUID");
                return true;
            }

            if (args.length != 1) {
                sd.error("只需要1个参数，提供了%d个参数".formatted(args.length));
                return true;
            }


            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final OfflinePlayer offlinePlayer = parseOfflinePlayerName(argPlayer, plugin.getServer());
                MojangProfileApi.Profile profile;
                if (offlinePlayer == null) {

                    sd.warning("找不到该玩家：%s，该玩家从未进入过服务器，正在联网查询正版玩家信息...".formatted(argPlayer));

                    try {
                        profile = plugin.getMojangProfileApi().requestByName(argPlayer);
                    } catch (Exception e) {
                        sd.warning("无法通过游戏名查询玩家信息！");
                        sd.exception(e);
                        return;
                    }
                } else {

                    String name = offlinePlayer.getName();
                    if (name != null) {
                        profile = new MojangProfileApi.Profile(name, offlinePlayer.getUniqueId());
                    } else {
                        sd.info("正在联网查询正版玩家信息...");

                        try {
                            profile = plugin.getMojangProfileApi().requestByUuid(offlinePlayer.getUniqueId());
                        } catch (Exception e) {
                            sd.warning("无法通过UUID查询玩家信息！");
                            sd.exception(e);
                            return;
                        }
                    }
                }

                final WhitelistApiImpl api = plugin.getWhitelistApi();
                if (api == null) {
                    sd.error("WhitelistApiImpl is null!");
                    return;
                }

                if (this.isAdd) {
                    this.doAdd(sd, profile, sender, argPlayer, api);
                } else {
                    this.doRemove(api, profile, sd, sender);
                }
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return tabCompleteOfflinePlayerNames(args[0], plugin.getServer(), false, "<玩家名或UUID>");
            }

            return null;
        }
    }


    // todo: 指令未测试
    private class Get extends NewMcCommand {

        private final @NotNull Permission permission;

        protected Get() {
            super("get");
            this.permission = this.addSubPermission(plugin.getServer().getPluginManager(), MainCommand.this.permission);
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            MainCommand.this.appendPrefix(text);
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            final String argPlayer = args.length > 0 ? args[0] : null;

            final Sender sd = new Sender(sender);

            if (argPlayer == null) {
                sd.error("必须提供参数：玩家名或UUID");
                return true;
            }

            if (args.length != 1) {
                sd.error("只需要1个参数，提供了%d个参数".formatted(args.length));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final OfflinePlayer offlinePlayer = parseOfflinePlayerName(argPlayer, plugin.getServer());
                if (offlinePlayer == null) {
                    sd.error("找不到玩家：%s，可能从未进入过服务器！".formatted(argPlayer));
                    return;
                }

                final WhitelistApiImpl api = plugin.getWhitelistApi();
                if (api == null) {
                    sd.error("WhitelistApiImpl is null!");
                    return;
                }

                final WhitelistInfo info;
                try {
                    info = api.getWhitelistService().query(offlinePlayer.getUniqueId());
                } catch (SQLException e) {
                    plugin.getSLF4JLogger().error("Fail to query whitelist", e);
                    sd.error("查询白名单失败！");
                    sd.exception(e);
                    return;
                }

                if (info == null) {
                    sd.info("玩家 %s 未添加白名单".formatted(argPlayer));
                    return;
                }

                final TextComponent.Builder text = Component.text();
                this.appendPrefix(text);
                text.appendSpace();
                text.append(Component.text("==== 白名单信息 ===="));
                Util.appendInfo(text, info, offlinePlayer.getName());
                sender.sendMessage(text.build().color(NamedTextColor.GREEN));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return tabCompleteOfflinePlayerNames(args[0], plugin.getServer(), false, "<玩家名或UUID>");
            }

            return null;
        }
    }

    // todo：未测试
    class Code extends NewMcCommand {

        private final @NotNull Permission permission;

        protected Code() {
            super("code");
            this.permission = this.addSubPermission(
                    plugin.getServer().getPluginManager(),
                    MainCommand.this.permission
            );
        }

        @Override
        protected void appendPrefix(TextComponent.@NotNull Builder text) {
            MainCommand.this.appendPrefix(text);
        }

        @Override
        protected boolean canExecute(@NotNull CommandSender commandSender) {
            return commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

            final Sender ms = new Sender(sender);

            if (!(sender instanceof final Player player)) {
                ms.error("该命令只能由玩家来执行！");
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final WhitelistApiImpl api = plugin.getWhitelistApi();
                if (api == null) {
                    ms.error("WhitelistApiImpl is null!");
                    return;
                }

                final WhitelistCodeInfo info;

                try {
                    info = api.getWhitelistCodeService().create(player.getUniqueId(), player.getName());
                } catch (SQLException e) {
                    plugin.getSLF4JLogger().error("Fail to crate whitelist code");
                    ms.error("生成白名单验证码失败！");
                    ms.exception(e);
                    return;
                }

                // 显示验证码
                final TextComponent.Builder text = Component.text();
                this.appendPrefix(text);
                text.appendSpace();
                text.append(Component.text("您的验证码为（可点击复制）："));
                text.append(Util.copyable("%d".formatted(info.code()))
                        .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
                text.append(Component.text("。"));
                text.append(Component.text("请不要向其他人泄露您的验证码，否则您的身份可能会被盗用！")
                        .color(NamedTextColor.RED));
                text.append(Component.text("验证码有效时间："));
                text.append(Component.text(Util.minutesAndSeconds(info.expires() - info.createTime()))
                        .color(NamedTextColor.YELLOW));
                text.append(Component.text("内，验证码被使用后立即失效。"));

                text.append(Component.text("刷新会生成新的验证码，并且原验证码立即失效"));

                final String cmd = "/%s %s".formatted(
                        MainCommand.this.getLabel(),
                        this.getLabel()
                );

                text.append(Component.text("[立即刷新]")
                        .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand(cmd))
                        .hoverEvent(HoverEvent.showText(Component.text("点击执行指令：" + cmd)))
                );

                sender.sendMessage(text.build().color(NamedTextColor.GREEN));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            return null;
        }
    }
}
