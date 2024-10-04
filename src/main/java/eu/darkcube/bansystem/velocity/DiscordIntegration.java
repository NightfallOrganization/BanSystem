/*
 * Copyright (c) 2024. [DarkCube]
 * All rights reserved.
 * You may not use or redistribute this software or any associated files without permission.
 * The above copyright notice shall be included in all copies of this software.
 */

package eu.darkcube.bansystem.velocity;

import eu.darkcube.discordbot.DiscordBot;

public class DiscordIntegration {
    public static void banLog(String message) {
        DiscordBot.logMessage(733562762898440202L, message);
    }

    public static void reportLog(String message) {
        DiscordBot.logMessage(1272095450299629598L, message);
    }
}
