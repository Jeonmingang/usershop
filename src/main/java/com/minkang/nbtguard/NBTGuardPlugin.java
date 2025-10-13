package com.minkang.nbtguard;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class NBTGuardPlugin extends JavaPlugin {

    private ProtocolManager pm;
    private boolean debug;
    private int maxStringBytes, maxLoreLines, maxLoreChars, maxDisplayNameChars;
    private boolean replaceIfStillTooBig;
    private NbtSanitizer sanitizer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();
        pm = ProtocolLibrary.getProtocolManager();

        pm.addPacketListener(new PacketAdapter(this, ListenerPriority.HIGH,
                PacketType.Play.Server.WINDOW_ITEMS,
                PacketType.Play.Server.SET_SLOT) {

            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    PacketContainer packet = event.getPacket();
                    if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                        List<ItemStack> list = packet.getItemListModifier().read(0);
                        if (list == null) return;
                        List<ItemStack> out = new ArrayList<>(list.size());
                        for (ItemStack it : list) {
                            out.add(process(it));
                        }
                        packet.getItemListModifier().write(0, out);
                    } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                        ItemStack it = packet.getItemModifier().read(0);
                        it = process(it);
                        packet.getItemModifier().write(0, it);
                    }
                } catch (Throwable t) {
                    if (debug) getLogger().warning("[NBTGuard] Packet handling error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
        });

        getLogger().info("[NBTGuard] Enabled.");
    }

    private ItemStack process(ItemStack it) {
        if (it == null) return null;
        ItemStack sanitized = sanitizer.sanitize(it);
        if (replaceIfStillTooBig && stillTooBig(sanitized)) {
            ItemStack barrier = new ItemStack(Material.BARRIER, sanitized.getAmount());
            return barrier;
        }
        return sanitized;
    }

    private boolean stillTooBig(ItemStack it) {
        try {
            int sum = 0;
            if (it.hasItemMeta()) {
                if (it.getItemMeta().hasDisplayName()) {
                    String s = it.getItemMeta().getDisplayName();
                    if (s != null) sum += s.getBytes(StandardCharsets.UTF_8).length;
                }
                if (it.getItemMeta().hasLore() && it.getItemMeta().getLore() != null) {
                    for (String l : it.getItemMeta().getLore()) {
                        if (l != null) sum += l.getBytes(StandardCharsets.UTF_8).length;
                    }
                }
            }
            return sum > maxStringBytes;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void reload() {
        FileConfiguration c = getConfig();
        debug = c.getBoolean("debug", true);
        maxStringBytes = c.getInt("limits.maxStringBytes", 60000);
        maxLoreLines = c.getInt("limits.maxLoreLines", 24);
        maxLoreChars = c.getInt("limits.maxLoreCharsPerLine", 220);
        maxDisplayNameChars = c.getInt("limits.maxDisplayNameChars", 120);
        replaceIfStillTooBig = c.getBoolean("replaceIfStillTooBig", true);
        sanitizer = new NbtSanitizer(this, maxStringBytes, maxLoreLines, maxLoreChars, maxDisplayNameChars);
    }
}
