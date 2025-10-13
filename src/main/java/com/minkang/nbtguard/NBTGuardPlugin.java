
package com.minkang.nbtguard;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class NBTGuardPlugin extends JavaPlugin {
    private ProtocolManager protocolManager;
    private FileConfiguration cfg;
    private int maxStringBytes, maxLoreLines, maxLoreLineLength, maxNameLen;
    private boolean trimName, debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        cfg = getConfig();
        maxStringBytes = cfg.getInt("maxStringBytes", 60000);
        maxLoreLines = cfg.getInt("maxLoreLines", 24);
        maxLoreLineLength = cfg.getInt("maxLoreLineLength", 220);
        trimName = cfg.getBoolean("trimDisplayName", true);
        maxNameLen = cfg.getInt("maxDisplayNameLength", 120);
        debug = cfg.getBoolean("debugLog", false);

        protocolManager = ProtocolLibrary.getProtocolManager();

        // WINDOW_ITEMS: whole inventory
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL,
                PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    PacketContainer packet = event.getPacket();
                    List<ItemStack> items;
                    try {
                        items = packet.getItemListModifier().read(0);
                    } catch (Exception e) {
                        items = null;
                    }
                    if (items != null) {
                        boolean changed = false;
                        for (int i = 0; i < items.size(); i++) {
                            ItemStack it = items.get(i);
                            ItemStack fixed = sanitize(it, event.getPlayer());
                            if (fixed != it) { items.set(i, fixed); changed = true; }
                        }
                        if (changed) {
                            packet.getItemListModifier().write(0, items);
                        }
                    }
                } catch (Throwable t) {
                    getLogger().log(Level.WARNING, "NBTGuard WINDOW_ITEMS error", t);
                }
            }
        });

        // SET_SLOT: single slot update
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL,
                PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    PacketContainer packet = event.getPacket();
                    ItemStack item = packet.getItemModifier().read(0);
                    ItemStack fixed = sanitize(item, event.getPlayer());
                    if (fixed != item) {
                        packet.getItemModifier().write(0, fixed);
                    }
                } catch (Throwable t) {
                    getLogger().log(Level.WARNING, "NBTGuard SET_SLOT error", t);
                }
            }
        });

        getLogger().info("NBTGuard enabled.");
    }

    private ItemStack sanitize(ItemStack item, Player player) {
        if (item == null) return null;
        try {
            ItemStack clone = item.clone();
            ItemMeta meta = clone.getItemMeta();
            boolean changed = false;

            if (meta != null) {
                // Trim display name
                if (trimName && meta.hasDisplayName()) {
                    String name = meta.getDisplayName();
                    if (name != null && name.length() > maxNameLen) {
                        meta.setDisplayName(name.substring(0, maxNameLen));
                        changed = true;
                    }
                }

                // Trim lore
                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null && !lore.isEmpty()) {
                        int limit = Math.min(lore.size(), maxLoreLines);
                        List<String> nl = new ArrayList<>(limit);
                        for (int i = 0; i < limit; i++) {
                            String s = String.valueOf(lore.get(i));
                            if (s.length() > maxLoreLineLength) {
                                s = s.substring(0, maxLoreLineLength);
                            }
                            nl.add(s);
                        }
                        if (!nl.equals(lore)) {
                            meta.setLore(nl);
                            changed = true;
                        }
                    }
                }

                // Remove oversize PDC strings
                try {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    if (pdc != null) {
                        Set<NamespacedKey> keys = pdc.getKeys();
                        if (keys != null) {
                            for (NamespacedKey key : new HashSet<>(keys)) {
                                String val = pdc.get(key, PersistentDataType.STRING);
                                if (val != null) {
                                    int bytes = val.getBytes(StandardCharsets.UTF_8).length;
                                    if (bytes > maxStringBytes) {
                                        pdc.remove(key);
                                        changed = true;
                                        if (debug) getLogger().info("Removed oversize PDC string: " + key + " (" + bytes + " bytes) for " + player.getName());
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable ignore) {}

                if (changed) {
                    clone.setItemMeta(meta);
                }

                // Final size check: if still too big (unknown NBT), replace with barrier to avoid kicks
                if (estimateItemNbtStringBytes(clone) > maxStringBytes) {
                    ItemStack barrier = new ItemStack(Material.BARRIER);
                    ItemMeta bm = barrier.getItemMeta();
                    if (bm != null) {
                        bm.setDisplayName(ChatColor.RED + "아이템 데이터가 너무 큽니다");
                        bm.setLore(Arrays.asList(ChatColor.GRAY + "서버 보호를 위해 표시만 대체되었습니다.",
                                ChatColor.GRAY + "원본 아이템은 서버 측 저장소에 있음."));
                        barrier.setItemMeta(bm);
                    }
                    return barrier;
                }
            }

            return changed ? clone : item;
        } catch (Throwable t) {
            if (debug) getLogger().log(Level.WARNING, "sanitize error", t);
            return item;
        }
    }

    // Heuristic: estimate length of concatenated name + lore + PDC strings
    private int estimateItemNbtStringBytes(ItemStack item) {
        try {
            ItemMeta meta = item.getItemMeta();
            int total = 0;
            if (meta != null) {
                if (meta.hasDisplayName() && meta.getDisplayName() != null)
                    total += meta.getDisplayName().getBytes(StandardCharsets.UTF_8).length;
                if (meta.hasLore() && meta.getLore() != null) {
                    for (String s : meta.getLore()) {
                        if (s != null) total += s.getBytes(StandardCharsets.UTF_8).length;
                    }
                }
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc != null) {
                    for (NamespacedKey key : pdc.getKeys()) {
                        String val = pdc.get(key, PersistentDataType.STRING);
                        if (val != null) total += val.getBytes(StandardCharsets.UTF_8).length;
                    }
                }
            }
            return total;
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            cfg = getConfig();
            sender.sendMessage(ChatColor.GREEN + "NBTGuard 설정을 리로드했습니다.");
            return true;
        }
        // Manual sanitize currently open inventory content (debug)
        if (p.getOpenInventory() != null && p.getOpenInventory().getTopInventory() != null) {
            Arrays.stream(p.getOpenInventory().getTopInventory().getContents())
                    .filter(Objects::nonNull)
                    .forEach(it -> sanitize(it, p));
            sender.sendMessage(ChatColor.YELLOW + "현재 GUI 아이템을 검사/정리했습니다.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "열려있는 GUI가 없습니다.");
        }
        return true;
    }
}
