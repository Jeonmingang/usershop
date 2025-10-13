package com.minkang.nbtguard;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class NbtSanitizer {

    private final JavaPlugin plugin;
    private final int maxStringBytes;
    private final int maxLoreLines;
    private final int maxLoreChars;
    private final int maxDisplayNameChars;

    public NbtSanitizer(JavaPlugin plugin, int maxStringBytes, int maxLoreLines, int maxLoreChars, int maxDisplayNameChars) {
        this.plugin = plugin;
        this.maxStringBytes = maxStringBytes;
        this.maxLoreLines = maxLoreLines;
        this.maxLoreChars = maxLoreChars;
        this.maxDisplayNameChars = maxDisplayNameChars;
    }

    public ItemStack sanitize(ItemStack in) {
        if (in == null) return null;
        try {
            ItemStack item = in.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;

            // Trim display name
            if (meta.hasDisplayName()) {
                String name = meta.getDisplayName();
                if (name != null && name.length() > maxDisplayNameChars) {
                    meta.setDisplayName(name.substring(0, maxDisplayNameChars));
                }
            }

            // Trim lore lines and lengths
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    List<String> out = new ArrayList<>(Math.min(lore.size(), maxLoreLines));
                    int lines = Math.min(lore.size(), maxLoreLines);
                    for (int i = 0; i < lines; i++) {
                        String s = lore.get(i);
                        if (s == null) continue;
                        if (s.length() > maxLoreChars) s = s.substring(0, maxLoreChars);
                        out.add(s);
                    }
                    meta.setLore(out);
                }
            }

            // Remove oversized STRING entries from PDC
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            try {
                Set<NamespacedKey> keys = pdc.getKeys();
                for (NamespacedKey key : keys) {
                    String v = pdc.get(key, PersistentDataType.STRING);
                    if (v != null) {
                        int bytes = v.getBytes(StandardCharsets.UTF_8).length;
                        if (bytes > maxStringBytes) {
                            pdc.remove(key);
                            plugin.getLogger().warning("[NBTGuard] Removed oversize PDC string key " + key + " (" + bytes + " bytes)");
                        }
                    }
                }
            } catch (Throwable ignored) {}

            item.setItemMeta(meta);
            return item;
        } catch (Throwable t) {
            plugin.getLogger().warning("[NBTGuard] sanitize failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return in;
        }
    }
}
