package com.minkang.ultimate.usershop.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
        return true;
    }
    public static boolean isMatching(ItemStack item, ConfigurationSection tmpl) {
        if (tmpl == null || item == null) return false;
        ItemMeta im = item.getItemMeta();
        String tName = tmpl.getString("name", "");
        List<String> tLore = tmpl.getStringList("lore");
        String iName = (im != null && im.hasDisplayName()) ? im.getDisplayName() : "";
        List<String> iLore = (im != null && im.hasLore()) ? im.getLore() : new ArrayList<>();
        return Objects.equals(iName, tName) && Objects.equals(iLore, tLore);
    }
}
