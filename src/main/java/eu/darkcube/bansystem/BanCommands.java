package eu.darkcube.bansystem;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.darkcube.system.libs.net.kyori.adventure.key.Key;
import eu.darkcube.system.libs.org.jetbrains.annotations.NotNull;
import eu.darkcube.system.libs.org.jetbrains.annotations.Nullable;
import eu.darkcube.system.userapi.User;
import eu.darkcube.system.util.data.DataKey;
import net.kyori.adventure.text.Component;

public class BanCommands {
    public static final String PERMISSION_BYPASS = "darkcube.ban.bypass";
    public static final String PERMISSION_COMMAND_BAN = "darkcube.command.ban";
    public static final String PERMISSION_COMMAND_UNBAN = "darkcube.command.unban";
    public static final String PERMISSION_COMMAND_REPORT = "darkcube.command.report";
    private static final SimpleCommandExceptionType NOT_A_VALID_SOURCE = new SimpleCommandExceptionType(() -> "You are not a valid CommandSource for this command!");
    public static final DataKey<Ban> KEY_BAN = DataKey.ofImmutable(Key.key("bansystem", "ban"), Ban.class);

    public static LiteralArgumentBuilder<CommandSource> createBan(ProxyServer server) {
        var userArgument = new UserArgument(server);
        // @formatter:off
        return literal("ban")
                .requires(source -> source.hasPermission(PERMISSION_COMMAND_BAN))
                .then(argument("target", StringArgumentType.word())
                        .suggests(userArgument::listSuggestions)
                        .executes(ctx -> ban(userArgument, server, ctx, null))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> ban(userArgument, server, ctx, StringArgumentType.getString(ctx, "reason")))
                        )
                );
        // @formatter:on
    }

    public static LiteralArgumentBuilder<CommandSource> createUnban(ProxyServer server) {
        var userArgument = new UserArgument(server);
        // @formatter:off
        return literal("unban")
                .requires(source -> source.hasPermission(PERMISSION_COMMAND_UNBAN))
                .then(argument("target", StringArgumentType.word())
                        .suggests(userArgument::listSuggestions)
                        .executes(ctx -> unban(userArgument, ctx))
                );
        // @formatter:on
    }

    public static LiteralArgumentBuilder<CommandSource> createReport(ProxyServer server) {
        var userArgument = new UserArgument(server);
        // @formatter:off
        return literal("report")
                .requires(source -> source.hasPermission(PERMISSION_COMMAND_REPORT))
                .then(argument("target", StringArgumentType.word())
                        .suggests(userArgument::listSuggestions)
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> report(userArgument, server, ctx, StringArgumentType.getString(ctx, "reason")))
                        )
                );
        // @formatter:on
    }

    private static int report(UserArgument userArgument, ProxyServer server, CommandContext<CommandSource> context, String reason) throws CommandSyntaxException {
        var userInput = context.getArgument("target", String.class);
        var user = userArgument.parse(new StringReader(userInput));
        var source = context.getSource();
        var reportedBy = name(source);
        if (reportedBy == null) throw NOT_A_VALID_SOURCE.create();
        if (source instanceof Player player && player.getUniqueId().equals(user.uniqueId())) {
            source.sendMessage(text("Du kannst dich selber nicht reporten!", RED));
            return 0;
        }
        var displayName = display(user);

        var message = "**" + displayName + "** wurde von " + reportedBy + " für `" + reason + "` reportet!";
        DiscordIntegration.reportLog(message);

        var msg = text(displayName, GOLD).append(text(" wurde von ", GRAY)).append(text(reportedBy, GOLD)).append(text(" für ", GRAY)).append(text(reason, DARK_PURPLE)).append(text(" reportet!", GRAY));
        for (var player : server.getAllPlayers()) {
            if (player.hasPermission(PERMISSION_COMMAND_BAN)) {
                player.sendMessage(msg);
            }
        }
        return 0;
    }

    private static int ban(UserArgument userArgument, ProxyServer server, CommandContext<CommandSource> context, @Nullable String reason) throws CommandSyntaxException {
        var userInput = context.getArgument("target", String.class);
        var user = userArgument.parse(new StringReader(userInput));
        var source = context.getSource();
        var bannedBy = name(source);
        if (bannedBy == null) throw NOT_A_VALID_SOURCE.create();

        var ban = new Ban(bannedBy, reason);
        user.persistentData().set(KEY_BAN, ban);

        var player = server.getPlayer(user.uniqueId());
        player.ifPresent(p -> {
            if (p.hasPermission(PERMISSION_BYPASS)) return;
            p.disconnect(banMessage(ban));
        });

        var message = display(user) + " wurde von " + bannedBy;
        if (reason != null) message += " für " + reason;
        message += " gebannt.";
        DiscordIntegration.banLog(message);
        source.sendMessage(text(display(user) + " was banned!"));
        return 0;
    }

    private static int unban(UserArgument userArgument, CommandContext<CommandSource> context) throws CommandSyntaxException {
        var userInput = context.getArgument("target", String.class);
        var user = userArgument.parse(new StringReader(userInput));
        var source = context.getSource();
        var unbannedBy = name(source);
        if (unbannedBy == null) throw NOT_A_VALID_SOURCE.create();
        user.persistentData().remove(KEY_BAN);

        DiscordIntegration.banLog(display(user) + " wurde von " + unbannedBy + " entbannt.");
        context.getSource().sendMessage(text(display(user) + " was unbanned!"));
        return 0;
    }

    private static String display(User user) {
        var name = user.name();
        if (user.uniqueId().toString().startsWith(name)) {
            return user.uniqueId().toString();
        }
        return name;
    }

    private static @Nullable String name(CommandSource source) {
        if (source instanceof Player player) {
            return player.getUsername();
        } else if (source instanceof ConsoleCommandSource) {
            return "console";
        }
        return null;
    }

    public static Component banMessage(Ban ban) {
        if (ban == null) {
            return text("You have been banned!");
        }
        var message = text("You have been banned!", RED);
        if (ban.reason != null) {
            message = message.append(newline()).append(text("Reason: ", RED).append(text(ban.reason, GOLD)));
        }
        message = message.append(newline()).append(newline());
        message = message.append(text("You can appeal at", RED)).append(newline()).append(text("https://discord.darkcube.eu/", DARK_PURPLE));
        return message;
    }

    public static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static RequiredArgumentBuilder<CommandSource, ?> argument(String name, ArgumentType<?> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public record Ban(@NotNull String bannedBy, @Nullable String reason) {
    }
}
