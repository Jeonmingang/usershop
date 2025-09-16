package com.minkang.ultimate.usershop.model;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerShop {
    private UUID owner;
    private int slots;
    // slot -> listing
    private Map<Integer, Listing> listings = new HashMap<>();

    public PlayerShop(UUID owner) {
        this.owner = owner;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getSlots() {
        return slots;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }

    public Map<Integer, Listing> getListings() {
        return listings;
    }

    public static PlayerShop fromYaml(YamlConfiguration yml) {
        UUID id = UUID.fromString(yml.getString("uuid"));
        PlayerShop ps = new PlayerShop(id);
        ps.slots = yml.getInt("slots", 9);
        for (String key : yml.getConfigurationSection("listings").getKeys(false)) {
            int slot = Integer.parseInt(key);
            ItemStack item = yml.getItemStack("listings." + key + ".item");
            double price = yml.getDouble("listings." + key + ".price");
            int stock = yml.getInt("listings." + key + ".stock");
            if (item != null) {
                ps.listings.put(slot, new Listing(item, price, stock));
            }
        }
        return ps;
    }

    public void toYaml(YamlConfiguration yml) {
        yml.set("uuid", owner.toString());
        yml.set("slots", slots);
        yml.createSection("listings");
        for (Map.Entry<Integer, Listing> e : listings.entrySet()) {
            String path = "listings." + e.getKey();
            yml.set(path + ".item", e.getValue().getItem());
            yml.set(path + ".price", e.getValue().getPrice());
            yml.set(path + ".stock", e.getValue().getStock());
        }
    }
}
