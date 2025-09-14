package com.minkang.ultimate.usershop.data;

import com.minkang.ultimate.usershop.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager {

    private final Main plugin;
    private File usersFile;
    private FileConfiguration usersCfg;

    public ShopManager(Main plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        usersFile = new File(dataFolder, "users.yml");
        if (!usersFile.exists()) {
            try { usersFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        usersCfg = YamlConfiguration.loadConfiguration(usersFile);
    }

    public void saveAll() {
        try { usersCfg.save(usersFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public int getCapacity(UUID uuid) {
        int def = plugin.getConfig().getInt("defaults.initial-slots", 9);
        return usersCfg.getInt("players." + uuid.toString() + ".slots", def);
    }

    public int addCapacity(UUID uuid, int add) {
        int cap = getCapacity(uuid) + add;
        int max = (plugin.getConfig().getInt("defaults.max-expansions", 5) + 1) * 9;
        if (cap > max) cap = max;
        usersCfg.set("players." + uuid.toString() + ".slots", cap);
        saveAll();
        return cap;
    }

    public int reduceCapacity(UUID uuid, int minus) {
        int cap = getCapacity(uuid) - minus;
        int min = plugin.getConfig().getInt("defaults.initial-slots", 9);
        if (cap < min) cap = min;
        usersCfg.set("players." + uuid.toString() + ".slots", cap);
        saveAll();
        return cap;
    }

    public void addListing(UUID uuid, int slot, double unitPrice, ItemStack stack) {
        String base = "players." + uuid.toString() + ".listings." + slot;
        usersCfg.set(base + ".price", unitPrice);
        usersCfg.set(base + ".item", stack);
        saveAll();
    }

    public ItemStack getListingItem(UUID uuid, int slot) {
        return usersCfg.getItemStack("players." + uuid.toString() + ".listings." + slot + ".item");
    }

    public Double getListingPrice(UUID uuid, int slot) {
        String path = "players." + uuid.toString() + ".listings." + slot + ".price";
        return usersCfg.contains(path) ? usersCfg.getDouble(path) : null;
    }

    public boolean removeListingToPlayer(UUID uuid, int slot, Player receiver) {
        String base = "players." + uuid.toString() + ".listings." + slot;
        if (!usersCfg.isConfigurationSection(base)) return false;
        ItemStack stack = usersCfg.getItemStack(base + ".item");
        usersCfg.set(base, null);
        saveAll();
        if (stack != null) {
            HashMap<Integer, ItemStack> left = receiver.getInventory().addItem(stack);
            if (!left.isEmpty()) {
                for (ItemStack s : left.values()) {
                    receiver.getWorld().dropItemNaturally(receiver.getLocation(), s);
                }
            }
        }
        return true;
    }

    public boolean adminRemoveListingToPlayer(UUID uuid, int slot, Player receiver) {
        return removeListingToPlayer(uuid, slot, receiver);
    }

    public static class MarketEntry {
        public UUID owner; public int slot; public double pricePerUnit; public ItemStack item; public String ownerName;
        public MarketEntry(UUID owner, int slot, double ppu, ItemStack item, String ownerName) { this.owner = owner; this.slot = slot; this.pricePerUnit = ppu; this.item = item; this.ownerName = ownerName; }
    }
    public static class OwnerEntry {
        public UUID owner; public String ownerName; public int itemCount; public double minPrice;
        public OwnerEntry(UUID owner, String name, int count, double minPrice) { this.owner = owner; this.ownerName = name; this.itemCount = count; this.minPrice = minPrice; }
    }

    public List<OwnerEntry> getOwners() {
        List<OwnerEntry> out = new ArrayList<>();
        ConfigurationSection playersSec = usersCfg.getConfigurationSection("players");
        if (playersSec == null) return out;
        for (String uuidStr : playersSec.getKeys(false)) {
            ConfigurationSection ls = usersCfg.getConfigurationSection("players." + uuidStr + ".listings");
            if (ls == null) continue;
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (Exception e) { continue; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = (op != null && op.getName() != null) ? op.getName() : uuid.toString();
            int count = 0; double min = Double.MAX_VALUE;
            for (String slotKey : ls.getKeys(false)) {
                String path = "players." + uuidStr + ".listings." + slotKey;
                double price = usersCfg.getDouble(path + ".price", 0.0);
                ItemStack item = usersCfg.getItemStack(path + ".item");
                if (item == null) continue;
                count++; if (price < min) min = price;
            }
            if (count > 0) {
                if (min == Double.MAX_VALUE) min = 0.0;
                out.add(new OwnerEntry(uuid, name, count, min));
            }
        }
        out.sort(Comparator.comparing(a -> a.ownerName.toLowerCase()));
        return out;
    }

    public List<MarketEntry> getOwnerListings(UUID owner, String query) {
        List<MarketEntry> out = new ArrayList<>();
        String base = "players." + owner.toString() + ".listings";
        ConfigurationSection ls = usersCfg.getConfigurationSection(base);
        if (ls == null) return out;
        String name = Optional.ofNullable(Bukkit.getOfflinePlayer(owner).getName()).orElse(owner.toString());
        for (String slotKey : ls.getKeys(false)) {
            String path = base + "." + slotKey;
            double price = usersCfg.getDouble(path + ".price", 0.0);
            ItemStack item = usersCfg.getItemStack(path + ".item"); if (item == null) continue;
            if (query != null && !query.isEmpty()) {
                String dname = (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
                        ? ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase()
                        : item.getType().name().toLowerCase();
                if (!dname.contains(query.toLowerCase())) continue;
            }
            int slot; try { slot = Integer.parseInt(slotKey); } catch (Exception e) { slot = -1; }
            out.add(new MarketEntry(owner, slot, price, item.clone(), name));
        }
        out.sort(Comparator.comparingDouble(a -> a.pricePerUnit));
        return out;
    }

    public List<MarketEntry> getAllListings(String query) {
        List<MarketEntry> out = new ArrayList<>();
        ConfigurationSection playersSec = usersCfg.getConfigurationSection("players");
        if (playersSec == null) return out;
        for (String uuidStr : playersSec.getKeys(false)) {
            ConfigurationSection ls = usersCfg.getConfigurationSection("players." + uuidStr + ".listings");
            if (ls == null) continue;
            UUID uuid; try { uuid = UUID.fromString(uuidStr); } catch (Exception e) { continue; }
            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString());
            for (String slotKey : ls.getKeys(false)) {
                String path = "players." + uuidStr + ".listings." + slotKey;
                double price = usersCfg.getDouble(path + ".price", 0.0);
                ItemStack item = usersCfg.getItemStack(path + ".item"); if (item == null) continue;
                if (query != null && !query.isEmpty()) {
                    String dname = (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
                            ? ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase()
                            : item.getType().name().toLowerCase();
                    if (!dname.contains(query.toLowerCase())) continue;
                }
                int slot; try { slot = Integer.parseInt(slotKey); } catch (Exception e) { slot = -1; }
                out.add(new MarketEntry(uuid, slot, price, item.clone(), name));
            }
        }
        out.sort(Comparator.comparingDouble(a -> a.pricePerUnit));
        return out;
    }

    public ItemStack takeFromListing(UUID owner, int slot, int qty) {
        String base = "players." + owner.toString() + ".listings." + slot;
        if (!usersCfg.isConfigurationSection(base)) return null;
        ItemStack stack = usersCfg.getItemStack(base + ".item"); if (stack == null) return null;
        int amount = stack.getAmount(); if (amount <= 0) return null;
        int take = Math.min(qty, amount);
        ItemStack give = stack.clone(); give.setAmount(take);
        int remain = amount - take;
        if (remain <= 0) usersCfg.set(base, null);
        else { stack.setAmount(remain); usersCfg.set(base + ".item", stack); }
        saveAll();
        return give;
    }
}
