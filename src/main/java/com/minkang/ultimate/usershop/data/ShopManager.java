package com.minkang.ultimate.usershop.data;

import com.minkang.ultimate.usershop.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager {

    public static class OwnerEntry {
        public final java.util.UUID owner;
        public final String name;
        public OwnerEntry(java.util.UUID owner) {
            this.owner = owner;
            String n = Bukkit.getOfflinePlayer(owner).getName();
            this.name = n == null ? owner.toString() : n;
        }
    }

    public static class MarketEntry {
        public final java.util.UUID owner;
        public final int slot;
        public final ItemStack item;
        public final double pricePerUnit;
        public MarketEntry(java.util.UUID owner, int slot, ItemStack item, double pricePerUnit) {
            this.owner = owner;
            this.slot = slot;
            this.item = item;
            this.pricePerUnit = pricePerUnit;
        }
    }

    private final Main plugin;
    private final File usersFile;
    private FileConfiguration usersCfg;

    public ShopManager(Main plugin) {
        this.plugin = plugin;
        this.usersFile = new File(plugin.getDataFolder(), "users.yml");
        reload();
    }

    public void reload() {
        try {
            if (!usersFile.getParentFile().exists()) usersFile.getParentFile().mkdirs();
            if (!usersFile.exists()) usersFile.createNewFile();
            this.usersCfg = YamlConfiguration.loadConfiguration(usersFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveAll() { try { usersCfg.save(usersFile); } catch (IOException e) { throw new RuntimeException(e); } }

    public java.util.List<OwnerEntry> listOwners() {
        java.util.List<OwnerEntry> out = new java.util.ArrayList<>();
        if (!usersCfg.isConfigurationSection("players")) return out;
        for (String key : Objects.requireNonNull(usersCfg.getConfigurationSection("players")).getKeys(false)) {
            try { java.util.UUID id = java.util.UUID.fromString(key); out.add(new OwnerEntry(id)); } catch (Exception ignored) {}
        }
        out.sort(java.util.Comparator.comparing(o -> o.name.toLowerCase(java.util.Locale.ROOT)));
        return out;
    }

    public int getOwnerMaxSlots(java.util.UUID owner) { return usersCfg.getInt("players." + owner + ".maxSlots", 9); }

    public void addSlots(java.util.UUID owner, int add) {
        int cur = getOwnerMaxSlots(owner);
        usersCfg.set("players." + owner + ".maxSlots", Math.max(0, cur + add));
        saveAll();
    }

    public MarketEntry getListing(java.util.UUID owner, int slot) {
        String base = "players." + owner + ".listings." + slot;
        if (!usersCfg.isConfigurationSection(base)) return null;
        ItemStack item = usersCfg.getItemStack(base + ".item");
        double price = usersCfg.getDouble(base + ".price", -1);
        if (item == null || price <= 0) return null;
        return new MarketEntry(owner, slot, item, price);
    }

    public ItemStack getListingItem(java.util.UUID owner, int slot) {
        MarketEntry me = getListing(owner, slot);
        return me == null ? null : me.item.clone();
    }

    public Double getListingPrice(java.util.UUID owner, int slot) {
        MarketEntry me = getListing(owner, slot);
        return me == null ? null : me.pricePerUnit;
    }

    public boolean setListing(java.util.UUID owner, int slot, ItemStack item, double pricePerUnit) {
        String base = "players." + owner + ".listings." + slot;
        if (item == null || pricePerUnit <= 0) return false;
        usersCfg.set(base + ".item", item.clone());
        usersCfg.set(base + ".price", pricePerUnit);
        saveAll();
        return true;
    }

    public boolean cancelListing(java.util.UUID owner, int slot) {
        String base = "players." + owner + ".listings." + slot;
        if (!usersCfg.isConfigurationSection(base)) return false;
        usersCfg.set(base, null);
        saveAll();
        return true;
    }

    public ItemStack takeFromListing(java.util.UUID owner, int slot, int qty) {
        String base = "players." + owner + ".listings." + slot;
        if (!usersCfg.isConfigurationSection(base)) return null;
        ItemStack stack = usersCfg.getItemStack(base + ".item");
        if (stack == null) return null;
        int amount = stack.getAmount();
        if (amount <= 0) return null;
        int take = Math.min(qty, amount);
        ItemStack give = stack.clone(); give.setAmount(take);
        int remain = amount - take;
        if (remain <= 0) usersCfg.set(base, null);
        else { stack.setAmount(remain); usersCfg.set(base + ".item", stack); }
        saveAll();
        return give;
    }

    public java.util.List<MarketEntry> listOwnerEntries(java.util.UUID owner) {
        java.util.List<MarketEntry> out = new java.util.ArrayList<>();
        String base = "players." + owner + ".listings";
        if (!usersCfg.isConfigurationSection(base)) return out;
        for (String k : Objects.requireNonNull(usersCfg.getConfigurationSection(base)).getKeys(false)) {
            try { int slot = Integer.parseInt(k); MarketEntry me = getListing(owner, slot); if (me != null) out.add(me); } catch (Exception ignored) {}
        }
        out.sort(java.util.Comparator.comparingInt(me -> me.slot));
        return out;
    }

    public java.util.List<MarketEntry> search(String query) {
        String qRaw = query == null ? "" : query;
        String q = org.bukkit.ChatColor.stripColor(qRaw).toLowerCase(java.util.Locale.ROOT);
        q = q.replace('_',' ').replaceAll("\s+"," ").trim();
        String qKo = com.minkang.ultimate.usershop.util.NameMap.en2ko(q);
        String qEn = com.minkang.ultimate.usershop.util.NameMap.ko2en(q);

        java.util.List<MarketEntry> out = new java.util.ArrayList<>();
        for (OwnerEntry oe : listOwners()) {
            for (MarketEntry me : listOwnerEntries(oe.owner)) {
                String en = me.item.getType().name();
                en = org.bukkit.ChatColor.stripColor(en).toLowerCase(java.util.Locale.ROOT).replace('_',' ').replaceAll("\s+"," ").trim();
                String ko = com.minkang.ultimate.usershop.util.NameMap.ko(me.item.getType());
                ko = org.bukkit.ChatColor.stripColor(ko).toLowerCase(java.util.Locale.ROOT).replace('_',' ').replaceAll("\s+"," ").trim();
                String dn = "";
                org.bukkit.inventory.meta.ItemMeta im = me.item.getItemMeta();
                if (im != null && im.getDisplayName()!=null) {
                    dn = org.bukkit.ChatColor.stripColor(im.getDisplayName()).toLowerCase(java.util.Locale.ROOT).replace('_',' ').replaceAll("\s+"," ").trim();
                }

                boolean match = false;
                if (!q.isEmpty()) {
                    if (en.contains(q) || ko.contains(q) || dn.contains(q)) match = true;
                    if (!match && en.contains(qEn)) match = true;
                    if (!match && ko.contains(qKo)) match = true;
                    if (!match && dn.contains(qKo)) match = true;
                } else {
                    match = true;
                }
                if (match) out.add(me);
            }
        }
        return out;
    }
}
