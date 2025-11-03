package com.minkang.wild;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomWildCommand implements CommandExecutor, TabCompleter {

    private final RandomWildPlugin plugin;

    public RandomWildCommand(RandomWildPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("wildrandom.others")) { sender.sendMessage("§c권한이 없습니다."); return true; }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { sender.sendMessage("§c해당 플레이어를 찾을 수 없습니다."); return true; }
        } else {
            if (!(sender instanceof Player)) { sender.sendMessage("§c콘솔은 /야생랜덤 <플레이어> 를 사용하세요."); return true; }
            target = (Player) sender;
        }
        TeleportFlow.start(plugin, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("wildrandom.others")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
