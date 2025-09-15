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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GUIManager {

    public enum Mode { MAIN, SHOP, SEARCH }

    private final Main plugin;
    private final ShopManager shopManager;
    private final NameMap nameMap;

    private final java.util.Map<java.util.UUID, ViewContext> views = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> pendingSearch = new java.util.HashSet<>();

    public GUIManager(Main plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.nameMap = new NameMap(plugin);
    }

    public static class ViewContext {
        public Mode mode;
        public int page;
        public String query;
        public UUID owner;
    }

    public ViewContext getViewContext(Player p) { return views.get(p.getUniqueId()); }

    public boolean isOurInventoryTitle(String title) {
        String s = ChatColor.stripColor(title == null ? "" : title);
        return s.contains("유저상점") || s.contains("검색 결과") || s.contains(" 님 상점");
    }
    public void clearView(Player p) { views.remove(p.getUniqueId()); }
    public void refresh(Player p) {
        ViewContext vc = views.get(p.getUniqueId());
        if (vc == null) return;
        if (vc.mode == Mode.MAIN) openMain(p, vc.page);
        else if (vc.mode == Mode.SHOP) openOwnerShop(p, vc.owner, vc.page);
        else if (vc.mode == Mode.SEARCH) openSearch(p, vc.page, vc.query);
    }
    public void requestSearch(Player p) {
        pendingSearch.add(p.getUniqueId());
        p.sendMessage(color("&b검색어를 채팅에 입력하세요. &7(취소: 취소)"));
    }
    public boolean handleChat(Player p, String msg) {
        if (!pendingSearch.contains(p.getUniqueId())) return false;
        pendingSearch.remove(p.getUniqueId());
        if (msg.equalsIgnoreCase("취소")) {
            p.sendMessage(color("&7검색이 취소되었습니다."));
            return true;
        }
        Bukkit.getScheduler().runTask(plugin, () -> openSearch(p, 1, msg));
        return true;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private ItemStack named(Material m, String name) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta(); im.setDisplayName(color(name)); is.setItemMeta(im);
        return is;
    }
    private void setPdc(ItemMeta im, String key, String value) {
        try {
            NamespacedKey k = new NamespacedKey(plugin, key);
            PersistentDataContainer pdc = im.getPersistentDataContainer();
            pdc.set(k, PersistentDataType.STRING, value);
        } catch (Throwable ignored) {}
        try { im.setLocalizedName(key + ":" + value); } catch (Throwable ignored) {}
    }

    private String bestItemName(ItemStack item) {
        String disp = (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
                ? ChatColor.stripColor(item.getItemMeta().getDisplayName())
                : null;
        if (disp != null && !disp.isEmpty()) return disp;
        return item.getType().name();
    }
    private String koreanName(ItemStack item) {
        String k = nameMap.koreanOfMaterial(item.getType().name());
        return k == null ? "" : k;
    }

    public void openMain(Player p, int page) {
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int size = rows * 9;
        String title = color(plugin.getConfig().getString("gui.titles.main", "&a유저상점 - 상점 목록 {page}")
                .replace("{page}", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(p, size, title);

        Material filler = Material.valueOf(plugin.getConfig().getString("gui.items.filler", "BLACK_STAINED_GLASS_PANE"));
        ItemStack fillerItem = new ItemStack(filler);
        ItemMeta fm = fillerItem.getItemMeta(); fm.setDisplayName(" "); fillerItem.setItemMeta(fm);
        for (int i=45;i<size;i++) inv.setItem(i, fillerItem);

        int prevSlot = plugin.getConfig().getInt("gui.controls.prev-slot", 45);
        int nextSlot = plugin.getConfig().getInt("gui.controls.next-slot", 53);
        int searchSlot = plugin.getConfig().getInt("gui.controls.search-slot", 49);
        inv.setItem(prevSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.prev", "ARROW")), "&7이전 페이지"));
        inv.setItem(nextSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.next", "ARROW")), "&7다음 페이지"));
        inv.setItem(searchSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.search", "COMPASS")), "&b검색"));

        java.util.List<OwnerEntry> owners = shopManager.getOwners();
        int start = (page - 1) * 45;
        int end = Math.min(start + 45, owners.size());
        int idx = 0;
        for (int i=start; i<end; i++) {
            OwnerEntry oe = owners.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            OfflinePlayer op = Bukkit.getOfflinePlayer(oe.owner);
            sm.setOwningPlayer(op);
            sm.setDisplayName(color("&e" + oe.ownerName + "&7의 상점"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.lore.head")) {
                String s = line.replace("{count}", String.valueOf(oe.itemCount))
                        .replace("{minPrice}", String.format("%.2f", oe.minPrice))
                        .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"));
                lore.add(color(s));
            }
            sm.setLore(lore);
            setPdc(sm, "usershop-owner", oe.owner.toString());
            head.setItemMeta(sm);
            inv.setItem(idx++, head);
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.MAIN; vc.page = page; vc.query = null; vc.owner = null;
        views.put(p.getUniqueId(), vc);
        p.\1
            try { com.minkang.ultimate.usershop.realtime.ShopRealtime.get().registerViewer("global", p); } catch (Throwable ignored) {}}

    public void openSearch(Player p, int page, String rawQuery) {
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int size = rows * 9;
        String title = color(plugin.getConfig().getString("gui.titles.search", "&d검색 결과 - '{query}' 페이지 {page}")
                .replace("{query}", rawQuery)
                .replace("{page}", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(p, size, title);

        Material filler = Material.valueOf(plugin.getConfig().getString("gui.items.filler", "BLACK_STAINED_GLASS_PANE"));
        ItemStack fillerItem = new ItemStack(filler);
        ItemMeta fm = fillerItem.getItemMeta(); fm.setDisplayName(" "); fillerItem.setItemMeta(fm);
        for (int i=45;i<size;i++) inv.setItem(i, fillerItem);

        int prevSlot = plugin.getConfig().getInt("gui.controls.prev-slot", 45);
        int nextSlot = plugin.getConfig().getInt("gui.controls.next-slot", 53);
        int searchSlot = plugin.getConfig().getInt("gui.controls.search-slot", 49);
        inv.setItem(prevSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.prev", "ARROW")), "&7이전 페이지"));
        inv.setItem(nextSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.next", "ARROW")), "&7다음 페이지"));
        inv.setItem(searchSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.search", "COMPASS")), "&b검색"));

        java.util.Set<String> norm = nameMap.normalizeQuery(rawQuery);
        java.util.List<MarketEntry> all = shopManager.getAllListings(null);
        java.util.List<MarketEntry> filtered = new java.util.ArrayList<>();
        String rq = rawQuery.toLowerCase();
        for (MarketEntry me : all) {
            String displayName = (me.item.hasItemMeta() && me.item.getItemMeta().hasDisplayName())
                    ? ChatColor.stripColor(me.item.getItemMeta().getDisplayName()).toLowerCase()
                    : "";
            String mat = me.item.getType().name().toLowerCase();
            String kor = nameMap.koreanOfMaterial(me.item.getType().name());
            boolean ok = false;
            if (!displayName.isEmpty() && displayName.contains(rq)) ok = true;
            if (mat.contains(rq)) ok = true;
            if (!ok && kor != null && !kor.isEmpty() && kor.replace(" ", "").toLowerCase().contains(rq.replace(" ", ""))) ok = true;
            if (!ok) {
                for (String q : norm) {
                    if (mat.contains(q) || (!displayName.isEmpty() && displayName.contains(q))) { ok = true; break; }
                    if (kor != null && !kor.isEmpty() && kor.replace(" ", "").toLowerCase().contains(q.replace(" ", ""))) { ok = true; break; }
                }
            }
            if (ok) filtered.add(me);
        }

        int start = (page - 1) * 45;
        int end = Math.min(start + 45, filtered.size());
        int idx = 0;
        for (int i=start; i<end; i++) {
            MarketEntry me = filtered.get(i);
            ItemStack display = me.item.clone();
            ItemMeta im = display.getItemMeta();
            String itemName = bestItemName(display);
            String korean = koreanName(display);
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.lore.item")) {
                String s = line.replace("{seller}", me.ownerName)
                        .replace("{price}", String.format("%.2f", me.pricePerUnit))
                        .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"))
                        .replace("{amount}", String.valueOf(me.item.getAmount()))
                        .replace("{item}", itemName)
                        .replace("{korean}", (korean == null || korean.isEmpty()) ? itemName : korean);
                lore.add(color(s));
            }
            im.setLore(lore);
            setPdc(im, "usershop-owner", me.owner.toString());
            setPdc(im, "usershop-slot", String.valueOf(me.slot));
            display.setItemMeta(im);
            inv.setItem(idx++, display);
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.SEARCH; vc.page = page; vc.query = rawQuery; vc.owner = null;
        views.put(p.getUniqueId(), vc);
        p.openInventory(inv);
        p.sendMessage(color("&7검색어: &f" + rawQuery + " &7결과: &f" + filtered.size() + "개"));
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

        Material filler = Material.valueOf(plugin.getConfig().getString("gui.items.filler", "BLACK_STAINED_GLASS_PANE"));
        ItemStack fillerItem = new ItemStack(filler);
        ItemMeta fm = fillerItem.getItemMeta(); fm.setDisplayName(" "); fillerItem.setItemMeta(fm);
        for (int i=45;i<size;i++) inv.setItem(i, fillerItem);

        int prevSlot = plugin.getConfig().getInt("gui.controls.prev-slot", 45);
        int nextSlot = plugin.getConfig().getInt("gui.controls.next-slot", 53);
        int searchSlot = plugin.getConfig().getInt("gui.controls.search-slot", 49);
        inv.setItem(prevSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.prev", "ARROW")), "&7이전 페이지"));
        inv.setItem(nextSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.next", "ARROW")), "&7다음 페이지"));
        inv.setItem(searchSlot, named(Material.valueOf(plugin.getConfig().getString("gui.items.search", "COMPASS")), "&b검색"));

        java.util.List<MarketEntry> list = shopManager.getAllListings(null);
        java.util.List<MarketEntry> mine = new java.util.ArrayList<>();
        for (MarketEntry me : list) if (me.owner.equals(owner)) mine.add(me);

        int start = (page - 1) * 45;
        int end = Math.min(start + 45, mine.size());
        int idx = 0;
        for (int i=start; i<end; i++) {
            MarketEntry me = mine.get(i);
            ItemStack display = me.item.clone();
            ItemMeta im = display.getItemMeta();
            String itemName = bestItemName(display);
            String korean = koreanName(display);
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.lore.item")) {
                String s = line.replace("{seller}", me.ownerName)
                        .replace("{price}", String.format("%.2f", me.pricePerUnit))
                        .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"))
                        .replace("{amount}", String.valueOf(me.item.getAmount()))
                        .replace("{item}", itemName)
                        .replace("{korean}", (korean == null || korean.isEmpty()) ? itemName : korean);
                lore.add(color(s));
            }
            im.setLore(lore);
            setPdc(im, "usershop-owner", me.owner.toString());
            setPdc(im, "usershop-slot", String.valueOf(me.slot));
            display.setItemMeta(im);
            inv.setItem(idx++, display);
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.SHOP; vc.page = page; vc.query = null; vc.owner = owner;
        views.put(p.getUniqueId(), vc);
        p.openInventory(inv);
    }
}