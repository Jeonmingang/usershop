package com.minkang.wild;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class WildInterceptListener implements Listener {

    private final RandomWildPlugin plugin;

    public WildInterceptListener(RandomWildPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onPreprocess(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg == null || !msg.startsWith("/")) return;
        String[] parts = msg.substring(1).trim().split("\\s+");
        if (parts.length == 0) return;
        String label = parts[0];
        if (!label.equalsIgnoreCase("야생랜덤") &&
            !label.equalsIgnoreCase("wildrandom") &&
            !label.equalsIgnoreCase("wild") || label.equalsIgnoreCase("랜덤야생")) return;

        Player p = e.getPlayer();
        if (parts.length >= 2) return; // 대상 지정은 원 명령어로 처리
        e.setCancelled(true);
        TeleportFlow.start(plugin, p);
    }
}
