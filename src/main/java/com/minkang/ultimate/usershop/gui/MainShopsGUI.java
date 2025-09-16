package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MainShopsGUI implements InventoryHolder {

    private java.util.List<java.util.UUID> pageOwners = new java.util.ArrayList<>();

    private final Main plugin;
    private final Player viewer;
    private Inventory inv;
    private int page;

    public MainShopsGUI(Main plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open(int page) {
        this.page = page;
        String title = Main.color(plugin.getConfig().getString("settings.titles.main", "유저 상점"));
        this.inv = Bukkit.createInventory(this, 54, title);
        fill(page);
        viewer.openInventory(inv);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    private void fill(int page) {
        pageOwners.clear();
        List<PlayerShop> shops = new ArrayList<>(plugin.getShopManager().allShops());
        // sort by owner name
        shops.sort((a,b)->{
            OfflinePlayer oa = Bukkit.getOfflinePlayer(a.getOwner());
            OfflinePlayer ob = Bukkit.getOfflinePlayer(b.getOwner());
            String na = oa.getName() == null ? a.getOwner().toString() : oa.getName();
            String nb = ob.getName() == null ? b.getOwner().toString() : ob.getName();
            return na.compareToIgnoreCase(nb);
        });
        int start = page * 45;
        int end = Math.min(start + 45, shops.size());
        for (int i = start; i < end; i++) {
            PlayerShop ps = shops.get(i);
            int slot = i - start;
            OfflinePlayer op = Bukkit.getOfflinePlayer(ps.getOwner());

            ItemStack head = ItemUtils.playerHead(op);
            ItemMeta meta = head.getItemMeta();
            String nameFmt = plugin.getConfig().getString("format.shop-head-name", "{player}님의 상점")
                    .replace("{player}", op.getName() == null ? ps.getOwner().toString() : op.getName());
            meta.setDisplayName(Main.color(nameFmt));
            List<String> lore = new ArrayList<>();
            for (String l : plugin.getConfig().getStringList("format.shop-head-lore")) {
                lore.add(Main.color(l.replace("{player}", op.getName() == null ? ps.getOwner().toString() : op.getName())));
            }
            meta.setLore(lore);
            head.setItemMeta(meta);

            inv.setItem(slot, head);
            pageOwners.add(ps.getOwner());
        }
        // bottom controls
        inv.setItem(plugin.getConfig().getInt("settings.icons.prev.slot",45), ItemUtils.iconFromCfg(plugin, "settings.icons.prev"));
        inv.setItem(plugin.getConfig().getInt("settings.icons.search.slot",49), ItemUtils.iconFromCfg(plugin, "settings.icons.search"));
        inv.setItem(plugin.getConfig().getInt("settings.icons.next.slot",53), ItemUtils.iconFromCfg(plugin, "settings.icons.next"));
    }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0) return;
        if (raw < 45) {
            if (raw >= pageOwners.size()) return;
            java.util.UUID ownerId = pageOwners.get(raw);
            new PlayerShopGUI(plugin, viewer, ownerId).open(0);
            return;
        }
        // legacy below (dead code)
            int index = page * 45 + raw;
            List<PlayerShop> shops = new ArrayList<>(plugin.getShopManager().allShops());
            if (index >= shops.size()) return;
            PlayerShop ps = shops.get(index);
            new PlayerShopGUI(plugin, viewer, ps.getOwner()).open(0);
            return;
        }
        int prevSlot = plugin.getConfig().getInt("settings.icons.prev.slot",45);
        int searchSlot = plugin.getConfig().getInt("settings.icons.search.slot",49);
        int nextSlot = plugin.getConfig().getInt("settings.icons.next.slot",53);
        if (raw == prevSlot) {
            int newPage = Math.max(0, page - 1);
            open(newPage);
        } else if (raw == searchSlot) {
            viewer.closeInventory();
            viewer.sendMessage(plugin.msg("search-type"));
            plugin.getShopManager().setWaitingSearch(viewer.getUniqueId(), true);
        } else if (raw == nextSlot) {
            // naive page increment
            open(page + 1);
        }
    }
}
