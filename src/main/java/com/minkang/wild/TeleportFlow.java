package com.minkang.wild;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TeleportFlow {

    public static void start(RandomWildPlugin plugin, Player p) {
        final World world = plugin.targetWorld();
        if (world == null) {
            p.sendMessage("§c대상 월드를 찾을 수 없습니다. config.yml 의 world 값을 확인하세요.");
            return;
        }

        long remain = plugin.getCooldowns().getRemaining(p);
        if (remain > 0) {
            long sec = Math.max(1, Math.round(remain / 1000.0));
            p.sendMessage("§e잠시 후 다시 사용 가능: §c" + sec + "초");
            return;
        }

        final AtomicReference<Location> found = new AtomicReference<>(null);
        final AtomicBoolean finished = new AtomicBoolean(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                Location safe = new RandomTeleporter().findSafe(plugin, world);
                found.set(safe);
                finished.set(true);
            }
        }.runTaskAsynchronously(plugin);

        final int total = Math.max(0, plugin.cfg().getInt("countdown-seconds", 1));
        final String titleCountdown = ChatColor.translateAlternateColorCodes('&',
                plugin.cfg().getString("title-countdown", "&e이동까지 &6{sec}&es"));
        final String titleSearching = ChatColor.translateAlternateColorCodes('&',
                plugin.cfg().getString("title-searching", "&e이동중..."));
        final String subSearching = ChatColor.translateAlternateColorCodes('&',
                plugin.cfg().getString("title-sub-found", "&7야생 좌표를 찾는 중"));
        final String titleDone = ChatColor.translateAlternateColorCodes('&',
                plugin.cfg().getString("title-done", "&a이동 완료!"));
        final String subDoneTpl = plugin.cfg().getString("title-sub-done", "&7(x:{x}, y:{y}, z:{z})");

        new BukkitRunnable() {
            int sec = total;
            @Override
            public void run() {
                if (sec > 0) {
                    String t = titleCountdown.replace("{sec}", String.valueOf(sec));
                    p.sendTitle(t, "§7이동 준비중...", 0, 10, 0);
                    sec--;
                    return;
                }
                if (!finished.get()) { p.sendTitle(titleSearching, subSearching, 0, 10, 0); return; }
                this.cancel();
                Location dest = found.get();
                if (dest == null) {
                    p.sendTitle("§c실패", "§7안전한 지점을 찾지 못했습니다. 잠시 후 재시도하세요.", 10, 30, 10);
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getCooldowns().stamp(p); // <-- stamp immediately upon success
                    p.teleport(dest);
                    String sub = subDoneTpl.replace("{x}", String.valueOf(dest.getBlockX()))
                                           .replace("{y}", String.valueOf(dest.getBlockY()))
                                           .replace("{z}", String.valueOf(dest.getBlockZ()));
                    p.sendTitle(titleDone, ChatColor.translateAlternateColorCodes('&', sub), 10, 30, 10);
                });
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
