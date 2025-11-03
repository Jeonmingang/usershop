package com.minkang.wild;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WhisperAliasCommand implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final RandomWildPlugin plugin;

    public WhisperAliasCommand(RandomWildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /" + label + " <플레이어> <내용>");
            return true;
        }
        String target = args[0];
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(' ');
            sb.append(args[i]);
        }
        String msg = sb.toString();

        // EssentialsX 등 /w 명령어 제공 플러그인으로 라우팅
        String dispatch = "w " + target + " " + msg;
        boolean ok = Bukkit.dispatchCommand(sender, dispatch);
        if (!ok) {
            sender.sendMessage("§c귓속말 명령어가 감지되지 않습니다. (/w 지원 플러그인이 필요합니다)");
        }
        return true;
    }


@Override
public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
    // 첫 번째 인자: 플레이어 자동완성
    if (args.length == 1) {
        String prefix = args[0].toLowerCase();
        java.util.List<String> list = new java.util.ArrayList<>();
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            String name = p.getName();
            if (prefix.isEmpty() || name.toLowerCase().startsWith(prefix)) list.add(name);
        }
        java.util.Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }
    return java.util.Collections.emptyList();
}

}
