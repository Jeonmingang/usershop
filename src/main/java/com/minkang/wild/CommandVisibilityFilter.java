package com.minkang.wild;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CommandVisibilityFilter implements Listener {

    private static final Set<String> HIDE_WILD = new HashSet<>(Arrays.asList(
            "야생랜덤", "wildrandom", "wild", "랜덤야생"
    ));
    private static final Set<String> HIDE_SHOUT = new HashSet<>(Arrays.asList(
            "확성기", "megaphone", "shout"
    ));

    @EventHandler
    public void onSend(PlayerCommandSendEvent e) {
        Player p = e.getPlayer();

        if (!p.hasPermission("wildrandom.visible")) {
            hide(e, HIDE_WILD);
        }
        if (!p.hasPermission("shout.visible")) {
            hide(e, HIDE_SHOUT);
        }
    }

    private void hide(PlayerCommandSendEvent e, Set<String> names) {
        e.getCommands().removeIf(cmd -> {
            String lower = cmd.toLowerCase();
            for (String n : names) {
                String nn = n.toLowerCase();
                if (lower.equals(nn) || lower.endsWith(":"+nn)) return true;
            }
            return false;
        });
    }
}
