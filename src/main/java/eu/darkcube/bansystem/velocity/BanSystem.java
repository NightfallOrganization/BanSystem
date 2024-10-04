/*
 * Copyright (c) 2024. [DarkCube]
 * All rights reserved.
 * You may not use or redistribute this software or any associated files without permission.
 * The above copyright notice shall be included in all copies of this software.
 */

package eu.darkcube.bansystem.velocity;

import static com.velocitypowered.api.event.ResultedEvent.ComponentResult.denied;
import static eu.darkcube.bansystem.velocity.BanCommands.createBan;
import static eu.darkcube.bansystem.velocity.BanCommands.createMute;
import static eu.darkcube.bansystem.velocity.BanCommands.createReport;
import static eu.darkcube.bansystem.velocity.BanCommands.createUmmute;
import static eu.darkcube.bansystem.velocity.BanCommands.createUnban;
import static eu.darkcube.bansystem.velocity.BanCommands.translate;
import static eu.darkcube.bansystem.Data.KEY_BAN;
import static eu.darkcube.bansystem.Data.banMessage;
import static eu.darkcube.bansystem.Permissions.PERMISSION_BYPASS;

import java.nio.file.Path;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.darkcube.system.userapi.UserAPI;
import javax.inject.Inject;

@Plugin(id = "bansystem", name = "BanSystem", authors = {"DasBabyPixel"}, version = "1.0", dependencies = {@Dependency(id = "cloudnet-bridge")})
public class BanSystem {
    private final ProxyServer server;
    private final Path dataDirectory;

    @Inject
    public BanSystem(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void handle(ProxyInitializeEvent event) {
        var commands = server.getCommandManager();
        commands.register(new BrigadierCommand(createBan(server)));
        commands.register(new BrigadierCommand(createUnban(server)));
        commands.register(new BrigadierCommand(createMute(server)));
        commands.register(new BrigadierCommand(createUmmute(server)));
        commands.register(new BrigadierCommand(createReport(server)));
    }

    @Subscribe
    public void handle(ProxyShutdownEvent event) {
        var commands = server.getCommandManager();
        commands.unregister("ban");
        commands.unregister("report");
        commands.unregister("unban");
    }

    @Subscribe
    public void handle(LoginEvent event) {
        var player = event.getPlayer();
        if (player.hasPermission(PERMISSION_BYPASS)) {
            return; // Do not process players with bypass permission
        }
        var user = UserAPI.instance().user(player.getUniqueId());
        var ban = user.persistentData().get(KEY_BAN);
        if (ban == null) {
            return; // User is not banned, return
        }
        event.setResult(denied(translate(banMessage(ban))));
    }
}
