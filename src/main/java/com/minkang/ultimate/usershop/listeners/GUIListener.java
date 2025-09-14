package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final Main plugin;
    private final ShopManager shopManager;
    private final GUIManager guiManager;

    public GUIListener(Main plugin, ShopManager shopManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        // Only our GUI
        if (!guiManager.isOurInventoryTitle(e.getView().getTitle())) return;

        int topSize = e.getView().getTopInventory().getSize();
        if (e.getRawSlot() < 0 || e.getRawSlot() >= topSize) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        // 구매 로직이 기존 다른 곳에 있으면 이 블록을 삭제해도 됨. 여기선 최소 구현만.
        if (!plugin.isEconomyReady()) return;
        // (생략) 실제 구매 처리 로직은 기존 것을 사용하세요.

        // After successful purchase:
        // guiManager.refresh(p);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player && guiManager.isOurInventoryTitle(e.getView().getTitle())) {
            guiManager.clearView((Player) e.getPlayer());
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
