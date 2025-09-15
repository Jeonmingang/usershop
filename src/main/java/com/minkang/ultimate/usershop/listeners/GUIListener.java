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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (!guiManager.hasView(p)) return;

        int topSize = e.getView().getTopInventory().getSize();
        if (e.getRawSlot() < 0 || e.getRawSlot() >= topSize) return; // only top
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        if (guiManager.handleControlClick(p, clicked, e.getRawSlot(), e.getView().getTitle())) return;

        ItemMeta im = clicked.getItemMeta();
        if (im == null) return;

        String ownerStr = null;
        String slotStr = null;
        try {
            ownerStr = im.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "usershop-owner"), org.bukkit.persistence.PersistentDataType.STRING);
            slotStr = im.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "usershop-slot"), org.bukkit.persistence.PersistentDataType.STRING);
        } catch (Throwable t) {}

        if (slotStr == null) { guiManager.handleContentClick(p, clicked); return; }

        if (!plugin.isEconomyReady()) { p.sendMessage(color("&c구매 기능은 Vault/Economy 플러그인이 있을 때만 동작합니다.")); return; }

        java.util.UUID owner; int slot;
        try { owner = java.util.UUID.fromString(ownerStr); } catch (Exception ex) { return; }
        try { slot = Integer.parseInt(slotStr); } catch (Exception ex) { return; }

        Double ppu = shopManager.getListingPrice(owner, slot);
        org.bukkit.inventory.ItemStack stored = shopManager.getListingItem(owner, slot);
        if (ppu == null || stored == null) { p.sendMessage(color("&c이미 판매 완료되었거나 존재하지 않습니다.")); return; }

        int qty = (e.getClick() == ClickType.SHIFT_LEFT) ? 64 : 1;
        int available = stored.getAmount();
        if (available <= 0) { p.sendMessage(color("&c재고가 없습니다.")); return; }
        if (qty > available) qty = available;

        double total = ppu * qty;
        net.milkbowl.vault.economy.Economy econ = plugin.getEconomy();
        if (!econ.has(p, total)) { p.sendMessage(color("&c잔액이 부족합니다. 필요: " + String.format("%.2f", total))); return; }

        econ.withdrawPlayer(p, total);
        OfflinePlayer sellerOffline = plugin.getServer().getOfflinePlayer(owner);
        econ.depositPlayer(sellerOffline, total);

        org.bukkit.inventory.ItemStack give = shopManager.takeFromListing(owner, slot, qty);
        if (give == null) { p.sendMessage(color("&c구매 실패: 재고 확인 중 오류.")); econ.depositPlayer(p, total); return; }

        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left = p.getInventory().addItem(give);
        if (!left.isEmpty()) for (org.bukkit.inventory.ItemStack s : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), s);

        p.sendMessage(color("&a구매 완료! &7구매 수량: &f" + qty + " &7총액: &e" + String.format("%.2f", total)));
        if (sellerOffline.isOnline()) {
            Player sp = sellerOffline.getPlayer();
            if (sp != null) sp.sendMessage(color("&e" + p.getName() + "&7님이 슬롯 " + slot + " 물품을 &e" + String.format("%.2f", total) + " &7에 구매 (수량 " + qty + ")"));
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
