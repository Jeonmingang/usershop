package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.gui.GUIManager.Mode;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class GUIListener implements Listener {

    private final Main plugin;
    private final ShopManager shopManager;
    private final GUIManager guiManager;

    public GUIListener(Main plugin, ShopManager shopManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.guiManager = guiManager;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!guiManager.isOurInventoryTitle(e.getView().getTitle())) return;

        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        GUIManager.ViewContext vc = guiManager.getViewContext(p);
        if (vc == null) return;

        // controls
        int prevSlot = plugin.getConfig().getInt("gui.controls.prev-slot", 45);
        int nextSlot = plugin.getConfig().getInt("gui.controls.next-slot", 53);
        int searchSlot = plugin.getConfig().getInt("gui.controls.search-slot", 49);
        int slot = e.getRawSlot();
        if (slot == prevSlot) { int newPage = Math.max(1, vc.page - 1); if (vc.mode == Mode.MAIN) guiManager.openMain(p, newPage); else if (vc.mode == Mode.SEARCH) guiManager.openSearch(p, newPage, vc.query); else if (vc.mode == Mode.SHOP) guiManager.openOwnerShop(p, vc.owner, newPage); return; }
        if (slot == nextSlot) { int newPage = vc.page + 1; if (vc.mode == Mode.MAIN) guiManager.openMain(p, newPage); else if (vc.mode == Mode.SEARCH) guiManager.openSearch(p, newPage, vc.query); else if (vc.mode == Mode.SHOP) guiManager.openOwnerShop(p, vc.owner, newPage); return; }
        if (slot == searchSlot) { guiManager.requestSearch(p); return; }

        ItemMeta im = clicked.getItemMeta();
        if (im == null) return;

        String ownerStr = null;
        String slotStr = null;
        try {
            ownerStr = im.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "usershop-owner"), PersistentDataType.STRING);
            slotStr = im.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "usershop-slot"), PersistentDataType.STRING);
        } catch (Throwable ignored) {}

        if (vc.mode == Mode.MAIN) {
            if (ownerStr != null) {
                try {
                    UUID owner = UUID.fromString(ownerStr);
                    guiManager.openOwnerShop(p, owner, 1);
                } catch (Exception ignored) {}
            }
            return;
        }

        if (vc.mode != Mode.SHOP) return;
        if (ownerStr == null || slotStr == null) return;

        UUID owner;
        int listingSlot;
        try { owner = UUID.fromString(ownerStr); } catch (Exception ex) { return; }
        try { listingSlot = Integer.parseInt(slotStr); } catch (Exception ex) { return; }

        Double ppu = shopManager.getListingPrice(owner, listingSlot);
        ItemStack stored = shopManager.getListingItem(owner, listingSlot);
        if (ppu == null || stored == null) { p.sendMessage(color("&c판매 정보가 없습니다.")); return; }

        int qty = (e.getClick() == ClickType.SHIFT_LEFT) ? 64 : 1;
        int available = stored.getAmount();
        if (available <= 0) { p.sendMessage(color("&c재고가 없습니다.")); return; }
        if (qty > available) qty = available;

        if (!plugin.isEconomyReady()) { p.sendMessage(color("&c결제(Economy)가 비활성화되어 구매할 수 없습니다.")); return; }

        double total = ppu * qty;
        net.milkbowl.vault.economy.Economy econ = plugin.getEconomy();
        if (!econ.has(p, total)) { p.sendMessage(color("&c잔액이 부족합니다. 필요: " + String.format("%.2f", total))); return; }

        econ.withdrawPlayer(p, total);
        OfflinePlayer sellerOffline = plugin.getServer().getOfflinePlayer(owner);
        econ.depositPlayer(sellerOffline, total);

        ItemStack give = shopManager.takeFromListing(owner, listingSlot, qty);
        if (give == null) { p.sendMessage(color("&c구매 실패: 재고 확인 중 오류.")); econ.depositPlayer(p, total); return; }
        p.getInventory().addItem(give);
        p.sendMessage(color("&a구매 완료! &7수량: " + qty + " / 합계: " + String.format("%.2f", total)));

        try { com.minkang.ultimate.usershop.realtime.ShopRealtime.get().onStockChanged("global", java.util.UUID.randomUUID()); } catch (Throwable ignored) {}
        guiManager.refresh(p);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        if (guiManager.isOurInventoryTitle(e.getView().getTitle())) {
            try { com.minkang.ultimate.usershop.realtime.ShopRealtime.get().unregisterViewer("global", p); } catch (Throwable ignored) {}
            guiManager.clearView(p);
        }
    }
}
