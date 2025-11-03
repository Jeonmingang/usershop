package com.minkang.wild;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final RandomWildPlugin plugin;

    public ReloadCommand(RandomWildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wildrandom.reload")) {
            sender.sendMessage("§c권한이 없습니다: wildrandom.reload");
            return true;
        }
        long t0 = System.currentTimeMillis();
        plugin.reloadConfig();
        // 재초기화가 필요한 매니저가 있다면 이곳에서 처리
        plugin.getLogger().info("Reload requested by " + sender.getName());
        long dt = System.currentTimeMillis() - t0;
        sender.sendMessage("§aWildRandom 설정을 리로드했습니다. (§7" + dt + "ms§a)");
        return true;
    }
}
