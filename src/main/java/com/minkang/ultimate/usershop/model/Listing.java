package com.minkang.ultimate.usershop.model;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class Listing {

    private ItemStack item;
    private double price;
    private int stock;

    public Listing(ItemStack item, double price, int stock) {
        this.item = item;
        this.price = price;
        this.stock = stock;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
