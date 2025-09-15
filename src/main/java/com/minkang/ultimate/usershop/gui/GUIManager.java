\1
import com.minkang.ultimate.usershop.realtime.ShopRealtime;
import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.data.ShopManager.MarketEntry;
import com.minkang.ultimate.usershop.data.ShopManager.OwnerEntry;
import com.minkang.ultimate.usershop.util.NameMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GUIManager {

    public enum Mode { MAIN, SHOP, SEARCH }

    private final Main plugin;
    private final ShopManager shopManager;
    private final NameMap nameMap;

    private final java.util.Map<java.util.UUID, PendingSearch> waitingSearch = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, ViewContext> views = new java.util.HashMap<>();

    public GUIManager(Main plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.nameMap = new NameMap(plugin);
    }

    public static class PendingSearch {
        public Mode fromMode;
        public UUID owner;
    }

    public static class ViewContext {
        public Mode mode;
        public int page;
        public String query;
        public UUID owner;
    }

    public boolean hasView(Player p) { return views.containsKey(p.getUniqueId()); }

    private ItemStack named(Material m, String name) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(color(name));
        is.setItemMeta(im);
        return is;
    }

    private void setPdc(ItemMeta im, String key, String value) {
        try {
            NamespacedKey k = new NamespacedKey(plugin, key);
            PersistentDataContainer pdc = im.getPersistentDataContainer();
            pdc.set(k, PersistentDataType.STRING, value);
        } catch (Throwable ignored) {}
        try {
            im.setLocalizedName(key + ":" + value); // fallback
        } catch (Throwable ignored) {}
    }

    private String getTag(ItemMeta im, String key) {
        try {
            NamespacedKey k = new NamespacedKey(plugin, key);
            PersistentDataContainer pdc = im.getPersistentDataContainer();
            String v = pdc.get(k, PersistentDataType.STRING);
            if (v != null) return v;
        } catch (Throwable ignored) {}
        try {
            String ln = im.getLocalizedName();
            if (ln != null && ln.startsWith(key + ":")) return ln.substring((key + ":").length());
        } catch (Throwable ignored) {}
        if (im.hasLore()) {
            for (String s : im.getLore()) {
                String raw = ChatColor.stripColor(s);
                String pre = "#TAG:" + key + "=";
                if (raw != null && raw.startsWith(pre)) {
                    return raw.substring(pre.length());
                }
            }
        }
        return null;
    }

    private void addKoreanLore(ItemMeta im, ItemStack item) {
        String kor = nameMap.koreanOfMaterial(item.getType().name());
        if (kor == null || kor.isEmpty()) return;
        List<String> lore = im.hasLore() ? im.getLore() : new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("gui.lore.item-korean")) {
            lore.add(color(line.replace("{kor}", kor)));
        }
        im.setLore(lore);
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
        ItemStack prev = named(Material.valueOf(plugin.getConfig().getString("gui.items.prev", "ARROW")), "&7이전 페이지");
        ItemStack next = named(Material.valueOf(plugin.getConfig().getString("gui.items.next", "ARROW")), "&7다음 페이지");
        ItemStack search = named(Material.valueOf(plugin.getConfig().getString("gui.items.search", "COMPASS")), "&b검색");

        inv.setItem(prevSlot, prev); inv.setItem(nextSlot, next); inv.setItem(searchSlot, search);

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
            lore.add(color("&0#TAG:usershop-owner=" + oe.owner.toString()));
            sm.setLore(lore);
            setPdc(sm, "usershop-owner", oe.owner.toString());
            head.setItemMeta(sm);
            inv.setItem(idx, head);
            idx++;
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.MAIN; vc.page = page; vc.query = null; vc.owner = null;
        views.put(p.getUniqueId(), vc);

        p.\1
            try { ShopRealtime.get().registerViewer("global", p); } catch (Throwable ignored) {}}

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
        ItemStack prev = named(Material.valueOf(plugin.getConfig().getString("gui.items.prev", "ARROW")), "&7이전 페이지");
        ItemStack next = named(Material.valueOf(plugin.getConfig().getString("gui.items.next", "ARROW")), "&7다음 페이지");
        ItemStack search = named(Material.valueOf(plugin.getConfig().getString("gui.items.search", "COMPASS")), "&b검색");

        inv.setItem(prevSlot, prev); inv.setItem(nextSlot, next); inv.setItem(searchSlot, search);

        java.util.List<MarketEntry> list = shopManager.getOwnerListings(owner, null);
        int start = (page - 1) * 45;
        int end = Math.min(start + 45, list.size());
        int idx = 0;
        for (int i=start; i<end; i++) {
            MarketEntry me = list.get(i);
            ItemStack display = me.item.clone();
            ItemMeta im = display.getItemMeta();
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.lore.item")) {
                String s = line.replace("{seller}", me.ownerName)
                        .replace("{price}", String.format("%.2f", me.pricePerUnit))
                        .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"))
                        .replace("{amount}", String.valueOf(me.item.getAmount()));
                lore.add(color(s));
            }
            im.setLore(lore);
            addKoreanLore(im, display);
            setPdc(im, "usershop-owner", me.owner.toString());
            setPdc(im, "usershop-slot", String.valueOf(me.slot));
            display.setItemMeta(im);
            inv.setItem(idx, display);
            idx++;
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.SHOP; vc.page = page; vc.query = null; vc.owner = owner;
        views.put(p.getUniqueId(), vc);

        p.openInventory(inv);
    }

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
        ItemStack prev = named(Material.valueOf(plugin.getConfig().getString("gui.items.prev", "ARROW")), "&7이전 페이지");
        ItemStack next = named(Material.valueOf(plugin.getConfig().getString("gui.items.next", "ARROW")), "&7다음 페이지");
        ItemStack search = named(Material.valueOf(plugin.getConfig().getString("gui.items.search", "COMPASS")), "&b검색");

        inv.setItem(prevSlot, prev); inv.setItem(nextSlot, next); inv.setItem(searchSlot, search);

        java.util.Set<String> norm = nameMap.normalizeQuery(rawQuery);

        java.util.List<MarketEntry> list = shopManager.getAllListings(null);
        java.util.List<MarketEntry> filtered = new java.util.ArrayList<>();
        for (MarketEntry me : list) {
            String displayName = (me.item.hasItemMeta() && me.item.getItemMeta().hasDisplayName())
                    ? ChatColor.stripColor(me.item.getItemMeta().getDisplayName()).toLowerCase()
                    : "";
            String mat = me.item.getType().name().toLowerCase();
            String kor = nameMap.koreanOfMaterial(me.item.getType().name());
            boolean ok = false;
            String rq = rawQuery.toLowerCase();
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
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String line : plugin.getConfig().getStringList("gui.lore.item")) {
                String s = line.replace("{seller}", me.ownerName)
                        .replace("{price}", String.format("%.2f", me.pricePerUnit))
                        .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"))
                        .replace("{amount}", String.valueOf(me.item.getAmount()));
                lore.add(color(s));
            }
            im.setLore(lore);
            addKoreanLore(im, display);
            setPdc(im, "usershop-owner", me.owner.toString());
            setPdc(im, "usershop-slot", String.valueOf(me.slot));
            display.setItemMeta(im);
            inv.setItem(idx, display);
            idx++;
        }

        ViewContext vc = new ViewContext();
        vc.mode = Mode.SEARCH; vc.page = page; vc.query = rawQuery; vc.owner = null;
        views.put(p.getUniqueId(), vc);

        p.openInventory(inv);
        p.sendMessage(color("&7검색어: &f" + rawQuery + " &7결과: &f" + filtered.size() + "개"));
    }

    public void beginSearch(Player p, Mode from, UUID owner) {
        PendingSearch ps = new PendingSearch();
        ps.fromMode = from; ps.owner = owner;
        waitingSearch.put(p.getUniqueId(), ps);
        p.closeInventory();
        p.sendMessage(color("&b검색어를 채팅으로 입력하세요. &7(취소: 취소, cancel)"));
    }

    public boolean isWaitingSearch(Player p) { return waitingSearch.containsKey(p.getUniqueId()); }

    public void handleSearchInput(Player p, String msg) {
        if (!isWaitingSearch(p)) return;
        PendingSearch ps = waitingSearch.remove(p.getUniqueId());
        if (msg.equalsIgnoreCase("취소") || msg.equalsIgnoreCase("cancel")) {
            p.sendMessage(color("&7검색이 취소되었습니다."));
            if (ps.fromMode == Mode.SHOP && ps.owner != null) openOwnerShop(p, ps.owner, 1);
            else openMain(p, 1);
            return;
        }
        openSearch(p, 1, msg);
    }

    public boolean handleControlClick(Player p, ItemStack clicked, int rawSlot, String title) {
        if (clicked == null) return false;
        String name = (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
        ViewContext vc = views.get(p.getUniqueId());
        if (vc == null) vc = new ViewContext();

        boolean isPrev = name.contains("이전 페이지");
        boolean isNext = name.contains("다음 페이지");
        boolean isSearch = name.contains("검색");

        if (isPrev) {
            int prev = vc.page - 1;
            if (prev < 1) { if (vc.mode == Mode.SHOP || vc.mode == Mode.SEARCH) openMain(p, 1); else p.sendMessage(color("&7첫 페이지입니다.")); return true; }
            if (vc.mode == Mode.MAIN) openMain(p, prev);
            else if (vc.mode == Mode.SHOP) openOwnerShop(p, vc.owner, prev);
            else if (vc.mode == Mode.SEARCH) openSearch(p, prev, vc.query);
            return true;
        }
        if (isNext) {
            int next = vc.page + 1;
            if (vc.mode == Mode.MAIN) openMain(p, next);
            else if (vc.mode == Mode.SHOP) openOwnerShop(p, vc.owner, next);
            else if (vc.mode == Mode.SEARCH) openSearch(p, next, vc.query);
            return true;
        }
        if (isSearch) { beginSearch(p, vc.mode, vc.owner); return true; }
        return false;
    }

    public void handleContentClick(Player p, ItemStack clicked) {
        if (clicked == null) return;
        ViewContext vc = views.get(p.getUniqueId());
        if (vc == null) return;
        ItemMeta im = clicked.getItemMeta();
        if (im == null) return;

        if (vc.mode == Mode.MAIN) {
            String ownerStr = getTag(im, "usershop-owner");
            if (ownerStr == null) return;
            try { openOwnerShop(p, UUID.fromString(ownerStr), 1); } catch (Exception ignored) {}
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
