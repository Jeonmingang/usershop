package com.minkang.ultimate.usershop.data;

import com.minkang.ultimate.usershop.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager {

    public static class OwnerEntry {
        public final UUID owner;
        public final String name;
        public OwnerEntry(UUID owner) {
            this.owner = owner;
            String n = Bukkit.getOfflinePlayer(owner).getName();
            this.name = n == null ? owner.toString() : n;
        }
    }

    public static class MarketEntry {
        public final UUID owner;
        public final int slot;
        public final ItemStack item;
        public final double pricePerUnit;
        public MarketEntry(UUID owner, int slot, ItemStack item, double pricePerUnit) {
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

    public void saveAll() {
        try { usersCfg.save(usersFile); } catch (IOException e) { throw new RuntimeException(e); }
    }

    public List<OwnerEntry> listOwners() {
        List<OwnerEntry> out = new ArrayList<>();
        if (!usersCfg.isConfigurationSection("players")) return out;
        for (String key : Objects.requireNonNull(usersCfg.getConfigurationSection("players")).getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                out.add(new OwnerEntry(id));
            } catch (Exception ignored) {}
        }
        out.sort(Comparator.comparing(o -> o.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    public int getOwnerMaxSlots(UUID owner) {
        return usersCfg.getInt("players." + owner + ".maxSlots", 9);
    }

    public void addSlots(UUID owner, int add) {
        int cur = getOwnerMaxSlots(owner);
        usersCfg.set("players." + owner + ".maxSlots", Math.max(0, cur + add));
        saveAll();
    }

    public MarketEntry getListing(UUID owner, int slot) {
        String base = "players." + owner + ".listings." + slot;
        if (!usersCfg.isConfigurationSection(base)) return null;
        ItemStack item = usersCfg.getItemStack(base + ".item");
        double price = usersCfg.getDouble(base + ".price", -1);
        if (item == null || price <= 0) return null;
        return new MarketEntry(owner, slot, item, price);
    }

    public ItemStack getListingItem(UUID owner, int slot) {
        MarketEntry me = getListing(owner, slot);
        return me == null ? null : me.item.clone();
    }

    public Double getListingPrice(UUID owner, int slot) {
        MarketEntry me = getListing(owner, slot);
        return me == null ? null : me.pricePerUnit;
    }

    public boolean setListing(UUID owner, int slot, ItemStack item, double pricePerUnit) {
        String base = "players." + owner + ".listings." + slot;
        if (item == null || pricePerUnit <= 0) return false;
        usersCfg.set(base + ".item", item.clone());
        usersCfg.set(base + ".price", pricePerUnit);
        saveAll();
        return true;
    }

    public boolean cancelListing(UUID owner, int slot) {
        String base = "players." + owner + ".listings." + slot;
        if (!usersCfg.isConfigurationSection(base)) return false;
        usersCfg.set(base, null);
        saveAll();
        return true;
    }

    public ItemStack takeFromListing(UUID owner, int slot, int qty) {
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

    public List<MarketEntry> listOwnerEntries(UUID owner) {
        List<MarketEntry> out = new ArrayList<>();
        String base = "players." + owner + ".listings";
        if (!usersCfg.isConfigurationSection(base)) return out;
        for (String k : Objects.requireNonNull(usersCfg.getConfigurationSection(base)).getKeys(false)) {
            try {
                int slot = Integer.parseInt(k);
                MarketEntry me = getListing(owner, slot);
                if (me != null) out.add(me);
            } catch (Exception ignored) {}
        }
        out.sort(Comparator.comparingInt(me -> me.slot));
        return out;
    }

    public List<MarketEntry> search(String query) {
        String q = ChatColor.stripColor(query == null ? "" : query).toLowerCase(Locale.ROOT);
        List<MarketEntry> out = new ArrayList<>();
        for (OwnerEntry oe : listOwners()) {
            for (MarketEntry me : listOwnerEntries(oe.owner)) {
                String name = me.item.getType().name().toLowerCase(Locale.ROOT);
                if (name.contains(q)) out.add(me);
            }
        }
        return out;
    }
}
