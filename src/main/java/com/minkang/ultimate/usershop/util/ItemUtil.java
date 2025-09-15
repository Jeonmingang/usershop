
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
        if (it == null || it.getType() == Material.AIR) return false;
        String name = tmpl.getString("name");
        if (name != null) {
            ItemMeta im = it.getItemMeta();
            if (im == null || im.getDisplayName() == null) return false;
            String disp = im.getDisplayName().replace('§','&');
            if (!disp.equals(name)) return false; // 완전 일치
        }
        List<String> lore = tmpl.getStringList("lore");
        if (lore != null && !lore.isEmpty()) {
            ItemMeta im = it.getItemMeta();
            if (im == null || im.getLore() == null) return false;
            List<String> got = new ArrayList<>();
            for (String s : Objects.requireNonNull(im.getLore())) got.add(s.replace('§','&'));
            if (got.size() != lore.size()) return false; // 길이까지 완전 일치
            for (int i=0;i<lore.size();i++) {
                if (!got.get(i).equals(lore.get(i))) return false;
            }
        }
        return true;
    }

    public static boolean consumeMatchingOne(Inventory inv, ConfigurationSection tmpl) {
        if (tmpl == null) return false;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (isMatching(it, tmpl)) {
                int amt = it.getAmount();
                if (amt > 1) { it.setAmount(amt - 1); inv.setItem(i, it); }
                else { inv.setItem(i, null); }
                return true;
            }
        }
        return false;
    }

    public static void saveTemplateFromHand(org.bukkit.entity.Player p, ConfigurationSection target) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) throw new IllegalArgumentException("손 아이템이 없습니다.");
        ItemMeta im = hand.getItemMeta();
        String name = im != null && im.getDisplayName()!=null ? im.getDisplayName().replace('§','&') : null;
        List<String> lore = new ArrayList<>();
        if (im != null && im.getLore() != null) {
            for (String s : im.getLore()) lore.add(s.replace('§','&'));
        }
        target.set("name", name);
        target.set("lore", lore);
    }
}
