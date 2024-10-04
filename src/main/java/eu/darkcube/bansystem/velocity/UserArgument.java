/*
 * Copyright (c) 2024. [DarkCube]
 * All rights reserved.
 * You may not use or redistribute this software or any associated files without permission.
 * The above copyright notice shall be included in all copies of this software.
 */

package eu.darkcube.bansystem.velocity;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.darkcube.system.userapi.User;
import eu.darkcube.system.userapi.UserAPI;

public class UserArgument implements ArgumentType<User> {
    private static final DynamicCommandExceptionType INVALID_PLAYER = new DynamicCommandExceptionType(player -> () -> "Invalid player: " + player);
    private static final PlayerManager PLAYER_MANAGER = InjectionLayer.boot().instance(ServiceRegistry.class).firstProvider(PlayerManager.class);
    private final ProxyServer server;

    public UserArgument(ProxyServer server) {
        this.server = server;
    }

    @Override
    public User parse(StringReader reader) throws CommandSyntaxException {
        var cursor = reader.getCursor();
        var string = reader.readUnquotedString();
        var playerOptional = server.getPlayer(string);
        if (playerOptional.isPresent()) {
            return UserAPI.instance().user(playerOptional.get().getUniqueId());
        }
        var offlinePlayer = PLAYER_MANAGER.firstOfflinePlayer(string);
        if (offlinePlayer != null) {
            return UserAPI.instance().user(offlinePlayer.uniqueId());
        }
        try {
            return UserAPI.instance().user(UUID.fromString(string));
        } catch (IllegalArgumentException e) {
            reader.setCursor(cursor);
            throw INVALID_PLAYER.createWithContext(reader, string);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var prefix = builder.getRemainingLowerCase();
        Player source = null;
        if (context.getSource() instanceof Player player) {
            source = player;
        }
        for (var player : server.getAllPlayers()) {
            if (source != null && player.getUniqueId().equals(source.getUniqueId())) continue;
            var name = player.getUsername();
            if (test(prefix, name.toLowerCase(Locale.ROOT))) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static boolean test(String input, String suggestion) {
        for (var i = 0; !suggestion.startsWith(input, i); ++i) {
            i = suggestion.indexOf(95, i);
            if (i < 0) {
                return false;
            }
        }
        return true;
    }
}
