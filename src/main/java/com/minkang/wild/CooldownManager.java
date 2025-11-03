package com.minkang.wild;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Plugin plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public CooldownManager(Plugin plugin) { this.plugin = plugin; }

    private int readCooldownSeconds() {
        FileConfiguration cfg = plugin.getConfig();
        // Primary key
        if (cfg.isInt("cooldown-seconds")) return cfg.getInt("cooldown-seconds");
        // Fallbacks (don't change config; just read if present)
        if (cfg.isInt("cooldown")) return cfg.getInt("cooldown");
        if (cfg.isInt("wild-cooldown-seconds")) return cfg.getInt("wild-cooldown-seconds");
        if (cfg.isInt("wild-cooldown")) return cfg.getInt("wild-cooldown");
        return 0;
    }

    public long getRemaining(Player p) {
        int cd = readCooldownSeconds();
        if (cd <= 0) return 0L;
        long now = System.currentTimeMillis();
        long next = lastUse.getOrDefault(p.getUniqueId(), 0L) + (cd * 1000L);
        return Math.max(0L, next - now);
    }

    public void stamp(Player p) { lastUse.put(p.getUniqueId(), System.currentTimeMillis()); }
}
