package com.minkang.ultimate.usershop.gui;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class StorageGUI implements InventoryHolder {

    private final Main plugin;
    private final Player viewer;
    private Inventory inv;

    public StorageGUI(Main plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public void open() {
        String title = Main.color("&e유저 상점 &7| &f보관함");
        inv = Bukkit.createInventory(this, 54, title);
        fill();
        viewer.openInventory(inv);
    }

    private void fill() {
        List<ItemStack> items = plugin.getShopManager().getStorage(viewer.getUniqueId());
        int i = 0;
        for (ItemStack it : items) {
            if (i >= 54) break;
            inv.setItem(i++, it);
        }
    }

    @Override
    public Inventory getInventory() { return inv; }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= inv.getSize()) return;
        ItemStack it = inv.getItem(raw);
        if (it == null) return;
        if (ItemUtils.giveItem(viewer, it.clone())) {
            plugin.getShopManager().removeFromStorage(viewer.getUniqueId(), it);
            inv.setItem(raw, null);
        }
    }
}
