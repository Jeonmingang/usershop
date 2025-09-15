package com.minkang.ultimate.usershop.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemUtil {

    public static boolean applyTemplateFromItem(org.bukkit.plugin.java.JavaPlugin plugin, String path, ItemStack hand) {
        if (hand == null || hand.getType() == Material.AIR) return false;
        ItemMeta im = hand.getItemMeta();
        String name = im != null && im.hasDisplayName() ? im.getDisplayName() : hand.getType().name();
        List<String> lore = im != null && im.hasLore() ? im.getLore() : new ArrayList<>();
        plugin.getConfig().set(path + ".name", name);
        plugin.getConfig().set(path + ".lore", lore);
        plugin.saveConfig();
        return true;
    }

    public static boolean isMatching(ItemStack item, ConfigurationSection tmpl) {
        if (tmpl == null || item == null) return false;
        ItemMeta im = item.getItemMeta();
        String tName = tmpl.getString("name", "");
        List<String> tLore = tmpl.getStringList("lore");

        String iName = (im != null && im.hasDisplayName()) ? im.getDisplayName() : "";
        List<String> iLore = (im != null && im.hasLore()) ? im.getLore() : new ArrayList<>();

        boolean nameEq = Objects.equals(iName, tName);
        boolean loreEq = Objects.equals(iLore, tLore);
        return nameEq && loreEq;
    }

    public static boolean consumeOneFromHandIfMatches(Player p, ConfigurationSection tmpl) {
        if (tmpl == null) return false;
        ItemStack it = p.getInventory().getItemInMainHand();
        if (it == null || it.getType() == Material.AIR) return false;
        if (!isMatching(it, tmpl)) return false;
        int amt = it.getAmount();
        if (amt > 1) { it.setAmount(amt - 1); p.getInventory().setItemInMainHand(it); }
        else { p.getInventory().setItemInMainHand(null); }
        return true;
    }

    public static boolean consumeMatchingOne(Inventory inv, ConfigurationSection tmpl) {
        if (tmpl == null) return false;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            if (!isMatching(it, tmpl)) continue;
            int amt = it.getAmount();
            if (amt > 1) { it.setAmount(amt - 1); inv.setItem(i, it); }
            else { inv.setItem(i, null); }
            return true;
        }
        return false;
    }
}
