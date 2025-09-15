package com.minkang.ultimate.usershop.util;

import org.bukkit.Material;

public class NameMap {
    public static String ko(Material m) {
        if (m == null) return "아이템";
        switch (m) {
            case DIAMOND: return "다이아몬드";
            case GOLD_INGOT: return "금 주괴";
            case IRON_INGOT: return "철 주괴";
            default: return m.name();
        }
    }
}
