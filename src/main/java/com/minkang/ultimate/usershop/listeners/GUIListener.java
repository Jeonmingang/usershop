package com.minkang.ultimate.usershop.listeners;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.gui.GUIManager.Mode;
import com.minkang.ultimate.usershop.util.NameMap;
import org.bukkit.Bukkit;
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
import org.bukkit.NamespacedKey;

public class GUIListener implements Listener {

    private final Main plugin;
    private final ShopManager shopManager;
    private final GUIManager guiManager;

    public GUIListener(Main plugin, ShopManager shopManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.guiManager = guiManager;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        if (!guiManager.isOurInventoryTitle(e.getView().getTitle())) return;

        int topSize = e.getView().getTopInventory().getSize();
        if (e.getRawSlot() < 0 || e.getRawSlot() >= topSize) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        String name = (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";

        if (name.contains("이전 페이지") || name.contains("다음 페이지") || name.contains("검색")) {
            GUIManager.ViewContext vc = guiManager.getViewContext(p);
            if (vc == null) return;
            if (name.contains("검색")) {
                p.closeInventory();
                guiManager.requestSearch(p);
                return;
            }
            int delta = name.contains("이전") ? -1 : +1;
            int newPage = Math.max(1, vc.page + delta);
            if (vc.mode == Mode.MAIN) guiManager.openMain(p, newPage);
            else if (vc.mode == Mode.SEARCH) guiManager.openSearch(p, newPage, vc.query);
            else if (vc.mode == Mode.SHOP) guiManager.openOwnerShop(p, vc.owner, newPage);
            return;
        }

        ItemMeta im = clicked.getItemMeta();
        if (im == null) return;

        String ownerStr = null;
        String slotStr = null;
        try {
            ownerStr = im.getPersistentDataContainer().get(new NamespacedKey(plugin, "usershop-owner"), PersistentDataType.STRING);
            slotStr = im.getPersistentDataContainer().get(new NamespacedKey(plugin, "usershop-slot"), PersistentDataType.STRING);
        } catch (Throwable t) {}

        if (ownerStr != null && (slotStr == null || slotStr.isEmpty())) {
            try { guiManager.openOwnerShop(p, java.util.UUID.fromString(ownerStr), 1); } catch (Exception ignored) {}
            return;
        }

        if (slotStr == null) return;

        if (!plugin.isEconomyReady()) { p.\1
            try { com.minkang.ultimate.usershop.realtime.ShopRealtime.get().onStockChanged("global", java.util.UUID.randomUUID()); } catch (Throwable ignored) {} return; }

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

        // Buyer notify
        p.sendMessage(color("&a구매 완료! &7구매 수량: &f" + qty + " &7총액: &e" + String.format("%.2f", total)));

        // Seller notify (online)
        if (plugin.getConfig().getBoolean("notify.sale.enabled", true)) {
            Player sellerOnline = Bukkit.getPlayer(owner);
            if (sellerOnline != null && sellerOnline.isOnline()) {
                String itemName = (stored.hasItemMeta() && stored.getItemMeta().hasDisplayName())
                        ? ChatColor.stripColor(stored.getItemMeta().getDisplayName())
                        : stored.getType().name();
                String kor = plugin.getConfig().getString("search.korean-materials." + stored.getType().name(), "");
                if (kor != null && !kor.isEmpty()) itemName = kor;
                String msg = plugin.getConfig().getString("notify.sale.format",
                        "&a{buyer}&7님이 당신의 상점에서 &f{item}&7을 &fx{qty}&7, 총 &e{total} {currency}&7(개당 {unit})에 구매했습니다.");
                msg = msg.replace("{buyer}", p.getName())
                        .replace("{item}", itemName)
                        .replace("{qty}", String.valueOf(qty))
                        .replace("{unit}", String.format("%.2f", ppu))
                        .replace("{total}", String.format("%.2f", total))
                        .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"));
                sellerOnline.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        }

        guiManager.refresh(p);
    }

    @EventHandler
    public void onClose(\1try { com.minkang.ultimate.usershop.realtime.ShopRealtime.get().unregisterViewer("global", (org.bukkit.entity.Player) e.getPlayer()); } catch (Throwable ignored) {}if (!(e.getPlayer() instanceof Player)) return;
        if (guiManager.isOurInventoryTitle(e.getView().getTitle())) {
            guiManager.clearView((Player) e.getPlayer());
        }
    }
}