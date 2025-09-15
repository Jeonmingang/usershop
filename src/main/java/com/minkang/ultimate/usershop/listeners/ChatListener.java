package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.gui.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final GUIManager gui;

    public ChatListener(com.minkang.ultimate.usershop.Main plugin, GUIManager gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!gui.isWaitingSearch(p)) return;
        e.setCancelled(true);
        org.bukkit.Bukkit.getScheduler().runTask(com.minkang.ultimate.usershop.Main.getInstance(), () -> {
            gui.handleSearchInput(p, e.getMessage());
        });
    }
}
