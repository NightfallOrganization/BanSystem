/*
 * Copyright (c) 2024. [DarkCube]
 * All rights reserved.
 * You may not use or redistribute this software or any associated files without permission.
 * The above copyright notice shall be included in all copies of this software.
 */

package eu.darkcube.bansystem;

import static eu.darkcube.system.libs.net.kyori.adventure.text.Component.newline;
import static eu.darkcube.system.libs.net.kyori.adventure.text.Component.text;
import static eu.darkcube.system.libs.net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;
import static eu.darkcube.system.libs.net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static eu.darkcube.system.libs.net.kyori.adventure.text.format.NamedTextColor.RED;

import eu.darkcube.system.libs.net.kyori.adventure.key.Key;
import eu.darkcube.system.libs.net.kyori.adventure.text.Component;
import eu.darkcube.system.util.data.DataKey;

public class Data {
    public static final DataKey<Ban> KEY_BAN = DataKey.ofImmutable(Key.key("bansystem", "ban"), Ban.class);
    public static final DataKey<Mute> KEY_MUTE = DataKey.ofImmutable(Key.key("bansystem", "mute"), Mute.class);

    public static Component banMessage(Ban ban) {
        if (ban == null) {
            return text("You have been banned!");
        }
        var message = text("You have been banned!", RED);
        if (ban.reason() != null) {
            message = message.append(newline()).append(text("Reason: ", RED).append(text(ban.reason(), GOLD)));
        }
        message = message.append(newline()).append(newline());
        message = message.append(text("You can appeal at", RED)).append(newline()).append(text("https://discord.darkcube.eu/", DARK_PURPLE));
        return message;
    }

    public static Component muteMessage(Mute mute) {
        if (mute == null) {
            return text("You have been muted!");
        }
        var message = text("You have been muted!", RED);
        if (mute.reason() != null) {
            message = message.append(newline()).append(text("Reason: ", RED).append(text(mute.reason(), GOLD)));
        }
        message = message.append(newline()).append(newline());
        message = message.append(text("You can appeal at", RED)).append(newline()).append(text("https://discord.darkcube.eu/", DARK_PURPLE));
        return message;
    }
}
