package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.data.ShopManager.MarketEntry;
import com.minkang.ultimate.usershop.data.ShopManager.OwnerEntry;
import com.minkang.ultimate.usershop.util.NameMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GUIManager {

    public enum Mode { MAIN, SEARCH, SHOP }

    public static class ViewContext {
        public Mode mode;
        public int page;
        public String query;
        public UUID owner;
    }

    private final Main plugin;
    private final ShopManager shop;
    private final Map<UUID, ViewContext> views = new HashMap<>();
    private final Set<UUID> pendingSearch = new HashSet<>();

    public GUIManager(Main plugin, ShopManager shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    public boolean isOurInventoryTitle(String title) {
        if (title == null) return false;
        String t = ChatColor.stripColor(title);
        return t.contains("상점") || t.toLowerCase(Locale.ROOT).contains("usershop");
    }

    public GUIManager.ViewContext getViewContext(Player p) { return views.get(p.getUniqueId()); }

    public void clearView(Player p) { views.remove(p.getUniqueId()); }

    public void refresh(Player p) {
        ViewContext vc = getViewContext(p);
        if (vc == null) return;
        switch (vc.mode) {
            case MAIN: openMain(p, vc.page); break;
            case SEARCH: openSearch(p, vc.page, vc.query); break;
            case SHOP: openOwnerShop(p, vc.owner, vc.page); break;
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    private void setPdc(ItemMeta im, String key, String val) {
        im.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, key), PersistentDataType.STRING, val);
    }

    public void requestSearch(Player p) {
        pendingSearch.add(p.getUniqueId());
        p.closeInventory();
        p.sendMessage(color("&7검색어를 채팅으로 입력하세요."));
    }

    public boolean handleChat(Player p, String msg) {
        if (!pendingSearch.remove(p.getUniqueId())) return false;
        Bukkit.getScheduler().runTask(plugin, () -> openSearch(p, 1, msg));
        return true;
    }

    public void openMain(Player p, int page) {
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int size = rows * 9;
        String title = color(plugin.getConfig().getString("gui.titles.main", "&b유저 상점 목록 - 페이지 {page}")
                .replace("{page}", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(p, size, title);

        List<OwnerEntry> owners = shop.listOwners();
        int start = (page - 1) * (size - 9);
        int end = Math.min(owners.size(), start + (size - 9));
        int idx = 0;
        for (int i=start; i<end; i++) {
            OwnerEntry oe = owners.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta im = head.getItemMeta();
            im.setDisplayName(color("&e" + oe.name));
            List<String> lore = Arrays.asList(color("&7클릭 시 상점 열기"));
            im.setLore(lore);
            setPdc(im, "usershop-owner", oe.owner.toString());
            head.setItemMeta(im);
            inv.setItem(idx++, head);
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.MAIN; vc.page = page; vc.query = null; vc.owner = null;
        views.put(p.getUniqueId(), vc);
        p.openInventory(inv);
        try { com.minkang.ultimate.usershop.realtime.ShopRealtime.get().registerViewer("global", p); } catch (Throwable ignored) {}
    }

    public void openSearch(Player p, int page, String rawQuery) {
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int size = rows * 9;
        String title = color(plugin.getConfig().getString("gui.titles.search", "&b검색 - 페이지 {page}")
                .replace("{page}", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(p, size, title);

        List<MarketEntry> all = shop.search(rawQuery);
        int start = (page - 1) * (size - 9);
        int end = Math.min(all.size(), start + (size - 9));
        int idx = 0;
        for (int i=start; i<end; i++) {
            MarketEntry me = all.get(i);
            ItemStack display = me.item.clone();
            ItemMeta im = display.getItemMeta();
            if (im != null) {
                String ko = NameMap.ko(display.getType());
                im.setDisplayName(color("&f" + ko + " &7x" + me.item.getAmount() + " &b" + String.format("%.2f", me.pricePerUnit) + "/개"));
                List<String> lore = new ArrayList<>();
                lore.add(color("&7판매자: &f" + Bukkit.getOfflinePlayer(me.owner).getName()));
                im.setLore(lore);
                setPdc(im, "usershop-owner", me.owner.toString());
                setPdc(im, "usershop-slot", String.valueOf(me.slot));
                display.setItemMeta(im);
            }
            inv.setItem(idx++, display);
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.SEARCH; vc.page = page; vc.query = rawQuery; vc.owner = null;
        views.put(p.getUniqueId(), vc);
        p.openInventory(inv);
        p.sendMessage(color("&7검색어: &f" + rawQuery + " &7결과: &f" + all.size() + "개"));
    }

    public void openOwnerShop(Player p, UUID owner, int page) {
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int size = rows * 9;
        String ownerName = String.valueOf(Bukkit.getOfflinePlayer(owner).getName());
        if (ownerName == null) ownerName = owner.toString();
        String title = color(plugin.getConfig().getString("gui.titles.shop", "&b{owner} 님 상점 - 페이지 {page}")
                .replace("{owner}", ownerName)
                .replace("{page}", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(p, size, title);

        List<MarketEntry> list = shop.listOwnerEntries(owner);
        int start = (page - 1) * (size - 9);
        int end = Math.min(list.size(), start + (size - 9));
        int idx = 0;
        for (int i=start; i<end; i++) {
            MarketEntry me = list.get(i);
            ItemStack display = me.item.clone();
            ItemMeta im = display.getItemMeta();
            if (im != null) {
                String ko = NameMap.ko(display.getType());
                im.setDisplayName(color("&f" + ko + " &7x" + me.item.getAmount() + " &b" + String.format("%.2f", me.pricePerUnit) + "/개"));
                List<String> lore = new ArrayList<>();
                lore.add(color("&7슬롯: &f" + me.slot));
                im.setLore(lore);
                setPdc(im, "usershop-owner", owner.toString());
                setPdc(im, "usershop-slot", String.valueOf(me.slot));
                display.setItemMeta(im);
            }
            inv.setItem(idx++, display);
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.SHOP; vc.page = page; vc.query = null; vc.owner = owner;
        views.put(p.getUniqueId(), vc);
        p.openInventory(inv);
    }
}
