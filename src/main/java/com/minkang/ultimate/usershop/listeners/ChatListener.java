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
        // 프로젝트의 기존 검색 입력 로직이 있으면 그걸 유지하세요.
    }
}
