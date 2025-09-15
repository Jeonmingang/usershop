
package com.minkang.ultimate.usershop.util;

import org.bukkit.Material;
import java.util.*;
import java.util.regex.*;

public class NameMap {
    private static final Map<String,String> EN2KO = new LinkedHashMap<>();
    static {
        // 기본 제공 맵 (필요 시 config로 확장됨)
        EN2KO.put("diamond", "다이아몬드");
        EN2KO.put("diamond sword", "다이아몬드 검");
        EN2KO.put("iron ingot", "철 주괴");
        EN2KO.put("gold ingot", "금 주괴");
        EN2KO.put("emerald", "에메랄드");
        EN2KO.put("coal", "석탄");
        EN2KO.put("redstone", "레드스톤");
        EN2KO.put("lapis lazuli", "청금석");

        // Pixelmon 예시(원하는 대로 config에서 확장 가능)
        EN2KO.put("pokeball", "몬스터볼");
        EN2KO.put("great ball", "수퍼볼");
        EN2KO.put("ultra ball", "울트라볼");
        EN2KO.put("master ball", "마스터볼");
        EN2KO.put("potion", "회복약");
        EN2KO.put("revive", "기력의조각");
    }

    private static Map<String,String> CONFIG_EN2KO = Collections.emptyMap();

    public static void loadConfig(java.util.Map<String,String> cfgMap) {
        if (cfgMap != null) {
            CONFIG_EN2KO = new LinkedHashMap<>();
            for (Map.Entry<String,String> e : cfgMap.entrySet()) {
                CONFIG_EN2KO.put(norm(e.getKey()), e.getValue());
            }
        }
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s;
        t = org.bukkit.ChatColor.stripColor(t);
        t = t.toLowerCase(java.util.Locale.ROOT);
        t = t.replace('_',' ').replaceAll("\\s+", " ").trim();
        return t;
    }

    public static String ko(Material m) {
        if (m == null) return "아이템";
        String en = norm(m.name());
        en = en.replace("minecraft ", "").replace("pixelmon ", "").replace("mod ", "");
        // try config first
        if (CONFIG_EN2KO.containsKey(en)) return CONFIG_EN2KO.get(en);
        // try built-in keywords by longest match
        String best = null;
        int bestLen = -1;
        for (Map.Entry<String,String> e : EN2KO.entrySet()) {
            String key = e.getKey();
            if (en.contains(key) && key.length() > bestLen) { best = e.getValue(); bestLen = key.length(); }
        }
        if (best != null) return best;
        return m.name();
    }

    public static String en2ko(String text) {
        String q = norm(text);
        // config exact/contains
        for (Map.Entry<String,String> e : CONFIG_EN2KO.entrySet()) {
            if (q.contains(e.getKey())) q = q.replace(e.getKey(), norm(e.getValue()));
        }
        for (Map.Entry<String,String> e : EN2KO.entrySet()) {
            if (q.contains(e.getKey())) q = q.replace(e.getKey(), norm(e.getValue()));
        }
        return q;
    }

    public static String ko2en(String text) {
        String q = norm(text);
        // reverse from both maps
        for (Map.Entry<String,String> e : CONFIG_EN2KO.entrySet()) {
            String ko = norm(e.getValue());
            if (q.contains(ko)) q = q.replace(ko, e.getKey());
        }
        for (Map.Entry<String,String> e : EN2KO.entrySet()) {
            String ko = norm(e.getValue());
            if (q.contains(ko)) q = q.replace(ko, e.getKey());
        }
        return q;
    }
}
