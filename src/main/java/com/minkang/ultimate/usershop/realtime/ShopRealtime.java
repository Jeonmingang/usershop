package com.minkang.ultimate.usershop.realtime;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopRealtime {
    private static volatile ShopRealtime INSTANCE;
    public static ShopRealtime get() {
        if (INSTANCE == null) {
            synchronized (ShopRealtime.class) {
                if (INSTANCE == null) INSTANCE = new ShopRealtime();
            }
        }
        return INSTANCE;
    }

    private final Map<String, Set<Player>> viewersByShop = new ConcurrentHashMap<>();
    private final Map<String, Integer> debounceTask = new ConcurrentHashMap<>();
    private org.bukkit.plugin.Plugin plugin;
    private boolean enabled = true;
    private int debounceTicks = 3;

    public void init(org.bukkit.plugin.Plugin plugin, boolean enabled, int debounceTicks) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.debounceTicks = Math.max(0, debounceTicks);
    }

    public void registerViewer(String shopId, Player p) {
        viewersByShop.computeIfAbsent(shopId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(p);
    }
    public void unregisterViewer(String shopId, Player p) {
        Set<Player> set = viewersByShop.get(shopId);
        if (set != null) {
            set.remove(p);
            if (set.isEmpty()) viewersByShop.remove(shopId);
        }
    }

    public void onStockChanged(String shopId, java.util.UUID itemId) {
        if (!enabled || plugin == null) return;
        if (debounceTask.containsKey(shopId)) return;
        int taskId = new BukkitRunnable() {
            @Override public void run() {
                debounceTask.remove(shopId);
                Set<Player> viewers = viewersByShop.get(shopId);
                if (viewers == null || viewers.isEmpty()) return;
                for (Player p : viewers) {
                    if (!p.isOnline()) continue;
                    try { p.updateInventory(); } catch (Throwable ignored) {}
                }
            }
        }.runTaskLater(plugin, debounceTicks).getTaskId();
        debounceTask.put(shopId, taskId);
    }

    public void broadcast(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(message);
    }
}