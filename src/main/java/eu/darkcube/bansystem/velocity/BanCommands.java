/*
 * Copyright (c) 2024. [DarkCube]
 * All rights reserved.
 * You may not use or redistribute this software or any associated files without permission.
 * The above copyright notice shall be included in all copies of this software.
 */

package eu.darkcube.bansystem.velocity;

import static eu.darkcube.bansystem.Data.KEY_BAN;
import static eu.darkcube.bansystem.Data.KEY_MUTE;
import static eu.darkcube.bansystem.Data.banMessage;
import static eu.darkcube.bansystem.Data.muteMessage;
import static eu.darkcube.bansystem.Permissions.PERMISSION_BYPASS;
import static eu.darkcube.bansystem.Permissions.PERMISSION_BYPASS_MUTE;
import static eu.darkcube.bansystem.Permissions.PERMISSION_COMMAND_BAN;
import static eu.darkcube.bansystem.Permissions.PERMISSION_COMMAND_MUTE;
import static eu.darkcube.bansystem.Permissions.PERMISSION_COMMAND_REPORT;
import static eu.darkcube.bansystem.Permissions.PERMISSION_COMMAND_UNBAN;
import static eu.darkcube.bansystem.Permissions.PERMISSION_COMMAND_UNMUTE;
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
import eu.darkcube.bansystem.Ban;
import eu.darkcube.bansystem.Mute;
import eu.darkcube.system.libs.org.jetbrains.annotations.Nullable;
import eu.darkcube.system.userapi.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class BanCommands {
    private static final SimpleCommandExceptionType NOT_A_VALID_SOURCE = new SimpleCommandExceptionType(() -> "You are not a valid CommandSource for this command!");

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

    public static LiteralArgumentBuilder<CommandSource> createMute(ProxyServer server) {
        var userArgument = new UserArgument(server);
        // @formatter:off
        return literal("mute")
                .requires(source -> source.hasPermission(PERMISSION_COMMAND_MUTE))
                .then(argument("target", StringArgumentType.word())
                        .suggests(userArgument::listSuggestions)
                        .executes(ctx -> mute(userArgument, server, ctx, null))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> mute(userArgument, server, ctx, StringArgumentType.getString(ctx, "reason")))
                        )
                );
        // @formatter:on
    }

    public static LiteralArgumentBuilder<CommandSource> createUmmute(ProxyServer server) {
        var userArgument = new UserArgument(server);
        // @formatter:off
        return literal("unmute")
                .requires(source -> source.hasPermission(PERMISSION_COMMAND_UNMUTE))
                .then(argument("target", StringArgumentType.word())
                        .suggests(userArgument::listSuggestions)
                        .executes(ctx -> unmute(userArgument, ctx))
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
            p.disconnect(translate(banMessage(ban)));
        });

        var message = display(user) + " wurde von " + bannedBy;
        if (reason != null) message += " für " + reason;
        message += " gebannt.";
        DiscordIntegration.banLog(message);
        source.sendMessage(text(display(user) + " was banned!"));
        return 0;
    }

    private static int mute(UserArgument userArgument, ProxyServer server, CommandContext<CommandSource> context, @Nullable String reason) throws CommandSyntaxException {
        var userInput = context.getArgument("target", String.class);
        var user = userArgument.parse(new StringReader(userInput));
        var source = context.getSource();
        var mutedBy = name(source);
        if (mutedBy == null) throw NOT_A_VALID_SOURCE.create();

        var mute = new Mute(mutedBy, reason);
        user.persistentData().set(KEY_MUTE, mute);

        var player = server.getPlayer(user.uniqueId());
        player.ifPresent(p -> {
            if (p.hasPermission(PERMISSION_BYPASS_MUTE)) return;
            p.sendMessage(translate(muteMessage(mute)));
        });

        var message = display(user) + " wurde von " + mutedBy;
        if (reason != null) message += " für " + reason;
        message += " gemuted.";
        DiscordIntegration.banLog(message);
        source.sendMessage(text(display(user) + " was muted!"));
        return 0;
    }

    public static Component translate(eu.darkcube.system.libs.net.kyori.adventure.text.Component o) {
        return GsonComponentSerializer.gson().deserialize(eu.darkcube.system.libs.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(o));
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

    private static int unmute(UserArgument userArgument, CommandContext<CommandSource> context) throws CommandSyntaxException {
        var userInput = context.getArgument("target", String.class);
        var user = userArgument.parse(new StringReader(userInput));
        var source = context.getSource();
        var unmutedBy = name(source);
        if (unmutedBy == null) throw NOT_A_VALID_SOURCE.create();
        user.persistentData().remove(KEY_MUTE);

        DiscordIntegration.banLog(display(user) + " wurde von " + unmutedBy + " entmuted.");
        context.getSource().sendMessage(text(display(user) + " was unmuted!"));
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

    public static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static RequiredArgumentBuilder<CommandSource, ?> argument(String name, ArgumentType<?> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
