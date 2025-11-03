package com.minkang.wild;

import net.md_5.bungee.api.ChatColor;

public class TextUtil {
    public static String colorize(String s, boolean allowColor, boolean allowHex) {
        if (s == null) return "";
        if (!allowColor) return ChatColor.stripColor(fromLegacy(s, allowHex));
        return fromLegacy(s, allowHex);
    }
    private static String fromLegacy(String s, boolean allowHex) {
        String out = s.replace('&', '§');
        if (allowHex) out = applyHex(out);
        else out = out.replaceAll("§#([A-Fa-f0-9]{6})", "");
        return out;
    }
    private static String applyHex(String s) {
        String out = s.replaceAll("(?i)&#([A-F0-9]{6})", "§x§$1").replaceAll("(?i)§#([A-F0-9]{6})", "§x§$1");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c=='§' && i+8<out.length() && out.charAt(i+1)=='x' && out.charAt(i+2)=='§') {
                String hex = out.substring(i+3, i+9);
                if (hex.length()==6) {
                    b.append("§x");
                    for (int j=0;j<6;j++) b.append('§').append(hex.charAt(j));
                    i += 8; continue;
                }
            }
            b.append(c);
        }
        return b.toString();
    }
}
