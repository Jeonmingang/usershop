package com.minkang.ultimate.usershop.util;

import com.minkang.ultimate.usershop.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Clean, compilable ItemUtils (no drops, no broken ellipsis).
 */
public class ItemUtils {

    /** Normalize string for search/filter. */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\p{M}+", "");
        return n.toLowerCase(Locale.ROOT);
    }

    /** Display-friendly name. */
    public static String getPrettyName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "AIR";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        if (meta instanceof SkullMeta) {
            OfflinePlayer op = ((SkullMeta) meta).getOwningPlayer();
            if (op != null) return "머리(" + op.getName() + ")";
        }
        return item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /** Compare similarity ignoring amount. */
    public static boolean isSimilarIgnoreAmount(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        ItemStack ca = a.clone(); ca.setAmount(1);
        ItemStack cb = b.clone(); cb.setAmount(1);
        return ca.isSimilar(cb);
    }

    /** Consume one matching item from player's inventory. */
    public static boolean consumeOne(Player p, ItemStack target) {
        if (p == null || target == null) return false;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null) continue;
            if (isSimilarIgnoreAmount(cur, target) && cur.getAmount() > 0) {
                if (cur.getAmount() == 1) inv.setItem(i, null);
                else { cur.setAmount(cur.getAmount() - 1); inv.setItem(i, cur); }
                return true;
            }
        }
        return false;
    }

    /** Give item; true if fully inserted (never drop). */
    public static boolean giveItem(Player p, ItemStack item) {
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item.clone());
        return leftover == null || leftover.isEmpty();
    }

    /** Give item and return leftover combined; null if fully inserted. */
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

    /** Build icon from config (material, name, lore, skull owner). */
    public static ItemStack iconFromCfg(ConfigurationSection sec) {
        String matName = sec.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;
        ItemStack item = new ItemStack(mat, sec.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (sec.isString("name")) meta.setDisplayName(Main.color(sec.getString("name")));
            if (sec.isList("lore")) {
                List<String> lore = new ArrayList<>();
                for (String l : sec.getStringList("lore")) lore.add(Main.color(l));
                meta.setLore(lore);
            }
            if (meta instanceof SkullMeta && sec.isString("owner")) {
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
