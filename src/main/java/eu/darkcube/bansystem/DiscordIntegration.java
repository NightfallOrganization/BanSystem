package eu.darkcube.bansystem;

import eu.darkcube.discordbot.DiscordBot;

public class DiscordIntegration {
    public static void banLog(String message) {
        DiscordBot.logMessage(733562762898440202L, message);
    }

    public static void reportLog(String message) {
        DiscordBot.logMessage(1272095450299629598L, message);
    }
}
