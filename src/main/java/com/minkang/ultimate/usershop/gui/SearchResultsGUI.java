package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.model.Listing;
import com.minkang.ultimate.usershop.model.PlayerShop;
import com.minkang.ultimate.usershop.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SearchResultsGUI implements InventoryHolder {
    private String normalize(String s) { if (s == null) return ""; return s.toLowerCase().replace(" ", ""); }
    private java.util.Set<String> expandQueries(String raw) {
        java.util.Set<String> out = new java.util.HashSet<>();
        String n = normalize(raw);
        out.add(n);
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        if (cfg.isConfigurationSection("translations")) {
            for (String k : cfg.getConfigurationSection("translations").getKeys(false)) {
                String v = cfg.getString("translations."+k, "");
                String kn = normalize(k); String vn = normalize(v);
                if (kn.equals(n)) out.add(vn);
                if (vn.equals(n)) out.add(kn);
            }
        }
        return out;
    }


    private final Main plugin;
    private final Player viewer;
    private final List<Result> results = new ArrayList<>();
    private Inventory inv;
    private int page;

    public static class Result {
        public UUID owner;
        public int slot;
        public Listing listing;
    }

    public SearchResultsGUI(Main plugin, Player viewer, String query) {
        this.plugin = plugin;
        this.viewer = viewer;
        // build results
        String q = ItemUtils.normalize(query);
        for (PlayerShop ps : plugin.getShopManager().allShops()) {
            for (Map.Entry<Integer, Listing> e : ps.getListings().entrySet()) {
                Listing l = e.getValue();
                String name = ItemUtils.getPrettyName(l.getItem());
                String norm = ItemUtils.normalize(name);
                if (norm.contains(q)) {
                    Result r = new Result();
                    r.owner = ps.getOwner();
                    r.slot = e.getKey();
                    r.listing = l;
                    results.add(r);
                }
            }
        }
    }

    public void open(int page) {
        this.page = page;
        String title = Main.color(plugin.getConfig().getString("settings.titles.search", "&e유저 상점 | {query} 검색 결과").replace("{query}", this.query));
        inv = Bukkit.createInventory(this, 54, title);
        fill(page);
        viewer.openInventory(inv);
    }

    private void fill(int page) {
        int start = page * 45;
        int end = Math.min(start + 45, results.size());
        int idx = 0;
        for (int i = start; i < end; i++) {
            Result r = results.get(i);
            int slot = idx++;
            ItemStack it = r.listing.getItem().clone();
            ItemMeta meta = it.getItemMeta();
            OfflinePlayer op = Bukkit.getOfflinePlayer(r.owner);
            String pretty = ItemUtils.getPrettyName(it);
            List<String> lore = new ArrayList<>();
            lore.add(Main.color(plugin.getConfig().getString("format.price", "가격: {price}").replace("{price}", String.valueOf(r.listing.getPrice()))));
            lore.add(Main.color(plugin.getConfig().getString("format.seller", "판매자: {seller}")
                    .replace("{seller}", op.getName()==null?op.getUniqueId().toString():op.getName())));
            lore.add(Main.color(plugin.getConfig().getString("format.stock", "재고: {stock}").replace("{stock}", String.valueOf(r.listing.getStock()))));
            meta.setLore(lore);
            it.setItemMeta(meta);
            inv.setItem(slot, it);
        }
        inv.setItem(plugin.getConfig().getInt("settings.icons.prev.slot",45), ItemUtils.iconFromCfg(plugin, "settings.icons.prev"));
        inv.setItem(plugin.getConfig().getInt("settings.icons.next.slot",53), ItemUtils.iconFromCfg(plugin, "settings.icons.next"));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0) return;
        int prevSlot = plugin.getConfig().getInt("settings.icons.prev.slot",45);
        int nextSlot = plugin.getConfig().getInt("settings.icons.next.slot",53);
        if (raw == prevSlot) {
            open(Math.max(0, page - 1));
            return;
        }
        if (raw == nextSlot) {
            open(page + 1);
            return;
        }
        int index = page * 45 + raw;
        if (index >= results.size()) return;
        Result r = results.get(index);
        // open that player's shop directly
        new PlayerShopGUI(plugin, viewer, r.owner).open(0);
    }
}
