
package com.minkang.ultimate.usershop.util;

import org.bukkit.ChatColor;

public final class Msg {
    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
