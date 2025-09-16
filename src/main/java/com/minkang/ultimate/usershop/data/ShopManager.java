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
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        Listing listing = new Listing(clone, price, amount);
        shop.getListings().put(slot, listing);
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
        boolean ok = ItemUtils.giveItem(buyer, give);
        if (!ok) {
            vault.deposit(buyer, total);
            if (seller != null) vault.withdraw(seller, total);
            buyer.sendMessage(Main.getInstance().msg("inventory-full"));
            return;
        }

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
}
