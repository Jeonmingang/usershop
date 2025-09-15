package com.minkang.ultimate.usershop.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemUtil {

    public static boolean isMatching(ItemStack it, ConfigurationSection tmpl) {
        if (tmpl == null) return false;
        String matStr = tmpl.getString("material");
        if (matStr != null) {
            try {
                Material m = Material.valueOf(matStr.toUpperCase());
                if (it.getType() != m) return false;
            } catch (Exception e) { return false; }
        }
        String name = tmpl.getString("name");
        if (name != null) {
            ItemMeta im = it.getItemMeta();
            if (im == null || im.getDisplayName() == null) return false;
            String disp = im.getDisplayName().replace('§','&');
            if (!disp.equalsIgnoreCase(name)) return false;
        }
        List<String> lore = tmpl.getStringList("lore");
        if (lore != null && !lore.isEmpty()) {
            ItemMeta im = it.getItemMeta();
            if (im == null || im.getLore() == null) return false;
            List<String> got = new ArrayList<>();
            for (String s : Objects.requireNonNull(im.getLore())) got.add(s.replace('§','&'));
            if (got.size() < lore.size()) return false;
            for (int i=0;i<lore.size();i++) {
                if (!got.get(i).equalsIgnoreCase(lore.get(i))) return false;
            }
        }
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
