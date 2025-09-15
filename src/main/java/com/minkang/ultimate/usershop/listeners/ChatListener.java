package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.gui.GUIManager;
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
        boolean handled = gui.handleChat(e.getPlayer(), e.getMessage());
        if (handled) e.setCancelled(true);
    }
}