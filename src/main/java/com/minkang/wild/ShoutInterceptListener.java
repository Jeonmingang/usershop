package com.minkang.wild;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ShoutInterceptListener implements Listener {

    private final RandomWildPlugin plugin;
    private final ShoutManager shoutMgr;

    public ShoutInterceptListener(RandomWildPlugin plugin, ShoutManager mgr) {
        this.plugin = plugin;
        this.shoutMgr = mgr;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreprocess(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg == null || !msg.startsWith("/")) return;
        String[] parts = msg.substring(1).trim().split("\\s+");
        if (parts.length == 0) return;
        String label = parts[0];
        if (!label.equalsIgnoreCase("확성기") &&
            !label.equalsIgnoreCase("megaphone") &&
            !label.equalsIgnoreCase("shout")) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        String[] args = new String[Math.max(0, parts.length - 1)];
        if (args.length > 0) System.arraycopy(parts, 1, args, 0, args.length);
        new ShoutCommand(plugin, shoutMgr).onCommand(p, null, label, args);
    }
}
