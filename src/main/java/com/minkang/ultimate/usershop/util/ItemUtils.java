package com.minkang.ultimate.usershop.util;

import com.minkang.ultimate.usershop.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Utility helpers for items & inventories.
 * This file replaces a broken version that contained ellipsis and unbalanced braces.
 */
public class ItemUtils {

    /** Normalize text for search (NFD + remove diacritics, toLower). */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT);
    }

    /** Pretty name for displaying an item. */
    public static String getPrettyName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "AIR";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        // Player head owner hint
        if (meta instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) meta;
            OfflinePlayer owner = sm.getOwningPlayer();
            if (owner != null) return "머리(" + owner.getName() + ")";
        }
        String mat = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return mat;
    }

    /** Compare two items ignoring amount. */
    public static boolean isSimilarIgnoreAmount(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        ItemStack ca = a.clone(); ca.setAmount(1);
        ItemStack cb = b.clone(); cb.setAmount(1);
        return ca.isSimilar(cb);
    }

    /** Remove one matching stack from player inventory; returns true if something was consumed. */
    public static boolean consumeOne(Player p, ItemStack target) {
        if (p == null || target == null) return false;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null) continue;
            if (isSimilarIgnoreAmount(cur, target) && cur.getAmount() > 0) {
                if (cur.getAmount() == 1) {
                    inv.setItem(i, null);
                } else {
                    cur.setAmount(cur.getAmount() - 1);
                    inv.setItem(i, cur);
                }
                return true;
            }
        }
        return false;
    }

    /** Give item; returns true when nothing left (NEVER drop). */
    public static boolean giveItem(Player p, ItemStack item) {
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item.clone());
        return leftover == null || leftover.isEmpty();
    }

    /** Give and return leftover stack combined; null when fully inserted. */
    public static ItemStack giveItemReturnLeftover(Player p, ItemStack item) {
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item.clone());
        if (leftover == null || leftover.isEmpty()) return null;
        ItemStack rem = null;
        for (ItemStack s : leftover.values()) {
            if (s == null) continue;
            if (rem == null) rem = s.clone();
            else rem.setAmount(rem.getAmount() + s.getAmount());
        }
        return rem;
    }

    /** Build an icon item from config: material, name, lore, skull-owner(optional). */
    public static ItemStack iconFromCfg(ConfigurationSection sec) {
        String matName = sec.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;
        ItemStack item = new ItemStack(mat, sec.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (sec.isString("name")) meta.setDisplayName(Main.color(sec.getString("name")));
            if (sec.isList("lore")) {
                List<String> colored = new ArrayList<>();
                for (String l : sec.getStringList("lore")) colored.add(Main.color(l));
                meta.setLore(colored);
            }
            if (meta instanceof SkullMeta && sec.isString("owner")) {
                // set skull owner if provided (1.16+ OwningPlayer is preferred)
                try {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(sec.getString("owner"));
                    ((SkullMeta) meta).setOwningPlayer(op);
                } catch (Throwable ignored) {}
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}