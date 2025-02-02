/*
 * Copyright (c) 2024. [DarkCube]
 * All rights reserved.
 * You may not use or redistribute this software or any associated files without permission.
 * The above copyright notice shall be included in all copies of this software.
 */

package eu.darkcube.bansystem.bukkit;

import static eu.darkcube.bansystem.Data.KEY_MUTE;
import static eu.darkcube.bansystem.Data.muteMessage;
import static eu.darkcube.bansystem.Permissions.PERMISSION_BYPASS_MUTE;

import eu.darkcube.system.userapi.UserAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BanSystemBukkit extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void handle(PlayerCommandPreprocessEvent event) {
        var player = event.getPlayer();
        if (player.hasPermission(PERMISSION_BYPASS_MUTE)) {
            return;
        }
        var msg = event.getMessage();
        if (msg.startsWith("/msg ") || msg.startsWith("/w ") || msg.startsWith("/tell ")) {
            var user = UserAPI.instance().user(player.getUniqueId());
            var mute = user.persistentData().get(KEY_MUTE);
            if (mute == null) {
                return;
            }
            event.setCancelled(true);
            user.sendMessage(muteMessage(mute));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void handle(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();

        if (player.hasPermission(PERMISSION_BYPASS_MUTE)) {
            return;
        }
        var user = UserAPI.instance().user(player.getUniqueId());
        var mute = user.persistentData().get(KEY_MUTE);
        if (mute == null) {
            return;
        }
        event.setCancelled(true);
        user.sendMessage(muteMessage(mute));
    }
}
