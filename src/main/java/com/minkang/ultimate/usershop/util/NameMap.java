package com.minkang.ultimate.usershop.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class NameMap {

    private final Map<String, String> canonToKo = new HashMap<>();
    private final Map<String, Set<String>> aliasToCanon = new HashMap<>();
    private final Map<String, String> materialKo = new HashMap<>(); // MATERIAL -> ko

    public NameMap(org.bukkit.plugin.java.JavaPlugin plugin) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("search.aliases");
        if (sec != null) {
            for (String canon : sec.getKeys(false)) {
                ConfigurationSection one = sec.getConfigurationSection(canon);
                if (one == null) continue;
                String ko = one.getString("ko", "");
                if (canon != null) canonToKo.put(canon.toLowerCase(), ko);

                Set<String> set = new HashSet<>();
                set.add(canon.toLowerCase());
                for (String a : one.getStringList("aliases")) set.add(a.toLowerCase());
                if (ko != null && !ko.isEmpty()) set.add(ko.toLowerCase());
                for (String a : set) aliasToCanon.computeIfAbsent(a, k -> new HashSet<>()).add(canon.toLowerCase());
            }
        }
        ConfigurationSection mk = plugin.getConfig().getConfigurationSection("search.korean-materials");
        if (mk != null) {
            for (String mat : mk.getKeys(false)) {
                String ko = mk.getString(mat, "");
                if (ko != null && !ko.isEmpty()) materialKo.put(mat.toUpperCase(), ko);
            }
        }
    }

    public Set<String> normalizeQuery(String input) {
        Set<String> out = new HashSet<>();
        if (input == null || input.trim().isEmpty()) return out;
        String q = input.toLowerCase();
        out.add(q);
        if (aliasToCanon.containsKey(q)) {
            out.addAll(aliasToCanon.get(q));
            for (String c : aliasToCanon.get(q)) out.add(c.toLowerCase());
        }
        return out;
    }

    public String koreanOfCanon(String canon) {
        if (canon == null) return null;
        return canonToKo.get(canon.toLowerCase());
    }

    public String koreanOfMaterial(String material) {
        if (material == null) return null;
        return materialKo.get(material.toUpperCase());
    }
}
