package com.minkang.ultimate.usershop.data;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.model.Listing;
import com.minkang.ultimate.usershop.model.PlayerShop;
import com.minkang.ultimate.usershop.util.ItemUtils;
import com.minkang.ultimate.usershop.util.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {
    private final java.util.Map<java.util.UUID, java.util.List<org.bukkit.inventory.ItemStack>> storage = new java.util.concurrent.ConcurrentHashMap<>();

    private final Main plugin;
    private final File dataDir;
    private final Map<UUID, PlayerShop> shops = new ConcurrentHashMap<>();
    private final Set<UUID> searchWaiting = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public ShopManager(Main plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "shops");
        if (!dataDir.exists()) dataDir.mkdirs();
    }

    public void loadAll() {
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                UUID id = UUID.fromString(yml.getString("uuid"));
                PlayerShop ps = PlayerShop.fromYaml(yml);
                shops.put(id, ps);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load shop file: " + f.getName() + " - " + ex.getMessage());
            }
        }
    }

    public void saveAll() {
        for (PlayerShop ps : shops.values()) {
            save(ps);
        }
    }

    private void save(PlayerShop ps) {
        try {
            File f = new File(dataDir, ps.getOwner().toString() + ".yml");
            YamlConfiguration yml = new YamlConfiguration();
            ps.toYaml(yml);
            yml.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save shop: " + e.getMessage());
        }
    }

    public PlayerShop getOrCreateShop(UUID uuid) {
        if (!shops.containsKey(uuid)) {
            PlayerShop ps = new PlayerShop(uuid);
            ps.setSlots(plugin.getConfig().getInt("settings.base-slots", 9));
            shops.put(uuid, ps);
        }
        return shops.get(uuid);
    }

    public PlayerShop getShop(UUID uuid) { return shops.get(uuid); }
    public Collection<PlayerShop> allShops() { return shops.values(); }

    public int getSlotCount(UUID uuid) {
        PlayerShop ps = getOrCreateShop(uuid);
        return ps.getSlots();
    }

    public int getMaxSlotsFor(UUID uuid) { return getSlotCount(uuid); }

    public void setSlotCount(UUID uuid, int slots) {
        PlayerShop ps = getOrCreateShop(uuid);
        ps.setSlots(slots);
        save(ps);
    }

    

public void registerListing(Player player, PlayerShop shop, ItemStack item, int amount, double price, int slot) {
        // SAFETY: prevent item loss when registering to an occupied slot.
        java.util.Map<Integer, com.minkang.ultimate.usershop.model.Listing> map = shop.getListings();
        com.minkang.ultimate.usershop.model.Listing prev = map.get(slot);
if (prev != null) {
    // Refund entire stock of previous listing into owner's storage
    org.bukkit.inventory.ItemStack template = prev.getItem().clone();
    int remaining = prev.getStock();
    int maxStack = template.getMaxStackSize();
    while (remaining > 0) {
        int chunk = Math.min(remaining, maxStack);
        org.bukkit.inventory.ItemStack part = template.clone();
        part.setAmount(chunk);
        addToStorage(shop.getOwner(), part);
        remaining -= chunk;
    }
}
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        com.minkang.ultimate.usershop.model.Listing listing = new com.minkang.ultimate.usershop.model.Listing(clone, price, amount, System.currentTimeMillis());
        map.put(slot, listing);
        save(shop);
    }



    public void unregisterListing(OfflinePlayer player, int slot, boolean refundItem, boolean refundTicket) {
        PlayerShop shop = getOrCreateShop(player.getUniqueId());
        Listing listing = shop.getListings().remove(slot);
        if (listing != null) {
            save(shop);
            if (player.isOnline()) {
                Player online = (Player) player;
                if (refundItem) ItemUtils.giveItem(online, listing.getItem());
                if (refundTicket) {
                    String b64 = plugin.getConfig().getString("items.register-ticket", "");
                    ItemStack ticket = com.minkang.ultimate.usershop.util.ItemSerializer.deserializeFromBase64(b64);
                    if (ticket != null) ItemUtils.giveItem(online, ticket);
                }
            }
        }
    }

    public void handlePurchase(Player buyer, UUID sellerId, int slot, int amount) {
        PlayerShop shop = getOrCreateShop(sellerId);
        Listing listing = shop.getListings().get(slot);
        if (listing == null) return;
        if (listing.getStock() <= 0) {
            buyer.sendMessage(Main.getInstance().msg("out-of-stock"));
            return;
        }
        int buyAmount = amount;
        if (buyAmount > 64) buyAmount = 64;
        if (buyAmount > listing.getStock()) buyAmount = listing.getStock();
        int maxStack = listing.getItem().getMaxStackSize();
        if (buyAmount > maxStack) buyAmount = maxStack;
        if (buyAmount <= 0) {
            buyer.sendMessage(Main.getInstance().msg("out-of-stock"));
            return;
        }

        double priceEach = listing.getPrice();
        double total = priceEach * buyAmount;

        VaultHook vault = plugin.getVault();
        if (vault == null || !vault.isOk()) {
            buyer.sendMessage(Main.color("&c경제 플러그인이 연결되어 있지 않습니다."));
            return;
        }

        if (!vault.has(buyer, total)) {
            buyer.sendMessage(Main.getInstance().msg("purchase-not-enough").replace("{need}", String.valueOf(total)));
            return;
        }

        if (!vault.withdraw(buyer, total)) {
            buyer.sendMessage(Main.getInstance().msg("purchase-not-enough").replace("{need}", String.valueOf(total)));
            return;
        }

        OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerId);
        vault.deposit(seller, total);

        ItemStack give = listing.getItem().clone();
give.setAmount(buyAmount);
// Deliver to personal storage instead of directly to inventory
addToStorage(buyer.getUniqueId(), give);

        listing.setStock(listing.getStock() - buyAmount);
        if (listing.getStock() <= 0) {
            shop.getListings().remove(slot);
        }
        save(shop);

        String itemName = ItemUtils.getPrettyName(give);
        buyer.sendMessage(Main.getInstance().msg("purchase-success")
                .replace("{item}", itemName)
                .replace("{amount}", String.valueOf(buyAmount))
                .replace("{paid}", String.valueOf(total)));
        if (seller.isOnline()) {
            Player sp = (Player) seller;
            sp.sendMessage(Main.getInstance().msg("seller-notify")
                    .replace("{buyer}", buyer.getName())
                    .replace("{item}", itemName)
                    .replace("{amount}", String.valueOf(buyAmount))
                    .replace("{paid}", String.valueOf(total)));
        }
    }

    public void setWaitingSearch(UUID id, boolean waiting) {
        if (waiting) searchWaiting.add(id);
        else searchWaiting.remove(id);
    }

    public boolean isWaitingSearch(UUID id) {
        return searchWaiting.contains(id);
    }
    
    public int getCapacity(java.util.UUID uuid) {
        PlayerShop ps = getOrCreateShop(uuid);
        return ps.getSlots();
    }

    public void setCapacity(java.util.UUID uuid, int cap) {
        PlayerShop ps = getOrCreateShop(uuid);
        ps.setSlots(cap);
        save(ps);
    }

    public java.util.List<org.bukkit.inventory.ItemStack> getStorage(java.util.UUID uuid) {
        return storage.computeIfAbsent(uuid, k -> new java.util.ArrayList<>());
    }

    public void addToStorage(java.util.UUID uuid, org.bukkit.inventory.ItemStack item) {
        getStorage(uuid).add(item.clone());
    }

    public void removeFromStorage(java.util.UUID uuid, org.bukkit.inventory.ItemStack item) {
        java.util.List<org.bukkit.inventory.ItemStack> lst = getStorage(uuid);
        for (int i=0;i<lst.size();i++) {
            org.bukkit.inventory.ItemStack it = lst.get(i);
            if (it.isSimilar(item) && it.getAmount() == item.getAmount()) {
                lst.remove(i);
                break;
            }
        }
    }

    public void sweepExpired() {
        int days = Main.getInstance().getConfig().getInt("expiry.days", 5);
        long ttl = days * 24L * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();
        java.util.List<PlayerShop> all = new java.util.ArrayList<>(shops.values());
        for (PlayerShop ps : all) {
            java.util.Map<Integer, com.minkang.ultimate.usershop.model.Listing> map = ps.getListings();
            java.util.List<Integer> toRemove = new java.util.ArrayList<>();
            for (java.util.Map.Entry<Integer, com.minkang.ultimate.usershop.model.Listing> e : map.entrySet()) {
                if (now - e.getValue().getCreatedAt() >= ttl) {
                    addToStorage(ps.getOwner(), e.getValue().getItem());
                    toRemove.add(e.getKey());
                }
            }
            for (Integer key : toRemove) map.remove(key);
            if (!toRemove.isEmpty()) save(ps);
        }
    }
}
