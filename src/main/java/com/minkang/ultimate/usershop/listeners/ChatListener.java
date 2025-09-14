package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.gui.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final GUIManager gui;
    public ChatListener(com.minkang.ultimate.usershop.Main plugin, GUIManager gui) { this.gui = gui; }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        // If your original has search-wait logic, keep using that; this is a no-op to avoid touching behavior.
    }
}
