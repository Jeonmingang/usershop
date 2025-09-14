package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    private final Main plugin;
    private final GUIManager gui;

    public InteractListener(Main plugin, GUIManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        boolean openOk = ItemUtil.isMatching(item, plugin.getConfig().getConfigurationSection("opener-item"));
        if (!openOk) return;
        e.setCancelled(true);
        gui.openMain(e.getPlayer(), 1);
    }
}
