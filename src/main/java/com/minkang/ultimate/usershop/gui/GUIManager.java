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

    // ==== Helpers ====
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
    private void addKoreanLore(ItemMeta im, ItemStack item) {
        String kor = nameMap.koreanOfMaterial(item.getType().name());
        if (kor == null || kor.isEmpty()) return;
        java.util.List<String> lore = im.hasLore() ? im.getLore() : new java.util.ArrayList<>();
        lore.add(color("&7한글명: &f" + kor));
        im.setLore(lore);
    }

    // ==== Main ====
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
        p.openInventory(inv);
    }

    public void openSearch(Player p, int page, String rawQuery) {
        // 프로젝트 원래 검색 구현이 있으면 그걸 쓰세요. 임시로 메인 재오픈 메시지만.
        openMain(p, 1);
        p.sendMessage(color("&7검색어: &f" + rawQuery));
    }

    public void openOwnerShop(Player p, UUID owner, int page) {
        // 프로젝트 원래 개별 상점 구현이 있으면 그걸 쓰세요. 임시로 메인 재오픈.
        openMain(p, 1);
    }
}
