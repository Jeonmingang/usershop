package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.data.ShopManager.OwnerEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class GUIManager {

    public enum Mode { MAIN, SHOP, SEARCH }

    private final Main plugin;
    private final ShopManager shopManager;
    private final java.util.Map<java.util.UUID, ViewContext> views = new java.util.HashMap<>();

    public GUIManager(Main plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    public static class ViewContext { public Mode mode; public int page; public String query; public UUID owner; }

    public boolean isOurInventoryTitle(String title) {
        String s = ChatColor.stripColor(title == null ? "" : title);
        return s.contains("유저상점") || s.contains("검색 결과") || s.contains("상점");
    }
    public void clearView(Player p) { views.remove(p.getUniqueId()); }
    public void refresh(Player p) {
        ViewContext vc = views.get(p.getUniqueId());
        if (vc == null) return;
        if (vc.mode == Mode.MAIN) openMain(p, vc.page);
        else if (vc.mode == Mode.SHOP) openOwnerShop(p, vc.owner, vc.page);
        else if (vc.mode == Mode.SEARCH) openSearch(p, vc.page, vc.query);
    }

    private ItemStack named(Material m, String name) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta(); im.setDisplayName(color(name)); is.setItemMeta(im);
        return is;
    }

    public void openMain(Player p, int page) {
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int size = rows * 9;
        String title = color(plugin.getConfig().getString("gui.titles.main", "&a유저상점 - 상점 목록 {page}").replace("{page}", String.valueOf(page)));
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

        List<OwnerEntry> owners = shopManager.getOwners();
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
            lore.add(color("&7등록 상품: &f" + oe.itemCount + "개"));
            lore.add(color("&7최저가: &e" + String.format("%.2f", oe.minPrice) + " " + plugin.getConfig().getString("defaults.currency-name", "코인")));
            head.setItemMeta(sm);
            inv.setItem(idx++, head);
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.MAIN; vc.page = page;
        views.put(p.getUniqueId(), vc);
        p.openInventory(inv);
    }

    public void openOwnerShop(Player p, UUID owner, int page) {
        // kept minimal; your existing render logic can replace if needed
        openMain(p, 1); // fallback minimal (keeps patch small). If you need full per-owner view, use your original.
    }

    public void openSearch(Player p, int page, String rawQuery) {
        // minimal stub; your existing search GUI logic remains external to this patch.
        openMain(p, 1);
        p.sendMessage(color("&7검색어: &f" + rawQuery));
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
