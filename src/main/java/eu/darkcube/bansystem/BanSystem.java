package eu.darkcube.bansystem;

import static com.velocitypowered.api.event.ResultedEvent.ComponentResult.denied;
import static eu.darkcube.bansystem.BanCommands.KEY_BAN;
import static eu.darkcube.bansystem.BanCommands.PERMISSION_BYPASS;
import static eu.darkcube.bansystem.BanCommands.banMessage;
import static eu.darkcube.bansystem.BanCommands.createBan;
import static eu.darkcube.bansystem.BanCommands.createReport;
import static eu.darkcube.bansystem.BanCommands.createUnban;

import java.nio.file.Path;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.Command;
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
        event.setResult(denied(banMessage(ban)));
    }
}
