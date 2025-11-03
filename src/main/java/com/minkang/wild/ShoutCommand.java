\
package com.minkang.wild;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ShoutCommand implements CommandExecutor, TabCompleter {
    private final RandomWildPlugin plugin;
    private final ShoutManager manager;

    public ShoutCommand(RandomWildPlugin plugin, ShoutManager mgr) {
        this.plugin = plugin;
        this.manager = mgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용할 수 있습니다."); return true; }
        Player p = (Player) sender;

        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("megaphone.enabled", true)) { p.sendMessage("§c확성기가 비활성화되어 있습니다."); return true; }
        if (args.length == 0) { p.sendMessage("§e사용법: /확성기 <메시지>"); return true; }

        String raw = String.join(" ", args);
        int maxLen = Math.max(10, cfg.getInt("megaphone.message-max-length", 120));
        if (raw.length() > maxLen) { p.sendMessage("§c메시지가 너무 깁니다. 최대 " + maxLen + "자"); return true; }

        // Cooldown
        int cd = Math.max(0, cfg.getInt("megaphone.cooldown-seconds", 30));
        if (!p.hasPermission("shout.bypass.cooldown")) {
            long remain = manager.remaining(p.getUniqueId(), cd);
            if (remain > 0) {
                long s = (long)Math.ceil(remain / 1000.0);
                p.sendMessage("§c쿨다운 " + s + "초 남았습니다.");
                return true;
            }
        }

        // Duplicate message prevention (simple: block immediate repeat)
        String last = manager.getLastMsg(p.getUniqueId());
        if (last != null && last.equalsIgnoreCase(raw) && cfg.getInt("megaphone.anti-duplicate-seconds", 10) > 0) {
            p.sendMessage("§c같은 메시지를 연속으로 사용할 수 없습니다.");
            return true;
        }

        // Cost
        double cost = cfg.getDouble("megaphone.cost", 0.0);
        boolean bypassCost = p.hasPermission("shout.bypass.cost");
        boolean charged = false;
        if (cost > 0.0 && !bypassCost) {
            if (!VaultHook.hasEconomy()) {
                p.sendMessage("§c경제 플러그인이 감지되지 않아 비용을 차감할 수 없습니다.");
                return true;
            }
            if (!VaultHook.economy().has(p, cost)) {
                sendInsufficient(p, cost);
                return true;
            }
            if (!VaultHook.economy().withdrawPlayer(p, cost).transactionSuccess()) {
                p.sendMessage("§c비용 차감에 실패했습니다.");
                return true;
            }
            charged = true;
        }

        boolean allowColor = cfg.getBoolean("megaphone.allow-color", true);
        boolean allowHex = cfg.getBoolean("megaphone.allow-hex", true);
        String prefix = cfg.getString("megaphone.prefix", "&6[확성기] &f{player}&7: ");
        String hover = cfg.getString("megaphone.hover", "&e{player}님의 확성기\n&7{time}");
        String suggest = cfg.getString("megaphone.click-suggest", "/귓 {player} ");

        String msgBody = TextUtil.colorize(raw, allowColor && p.hasPermission("shout.color"), allowHex && p.hasPermission("shout.color"));

        String built = TextUtil.colorize(prefix, true, true)
                .replace("{player}", p.getName())
                + msgBody;

        TextComponent comp = new TextComponent(TextComponent.fromLegacyText(built));
        TextComponent hoverComp = new TextComponent(TextUtil.colorize(hover, true, true)
                .replace("{player}", p.getName())
                .replace("{time}", new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date())));
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverComp).create()));
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest.replace("{player}", p.getName())));

        for (Player t : Bukkit.getOnlinePlayers()) t.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.CHAT, comp);

        // Sound
        if (cfg.getBoolean("megaphone.sound.enabled", true)) {
            String name = cfg.getString("megaphone.sound.name", "BLOCK_BELL_USE");
            float vol = (float) cfg.getDouble("megaphone.sound.volume", 0.7);
            float pit = (float) cfg.getDouble("megaphone.sound.pitch", 1.0);
            try {
                Sound s = Sound.valueOf(name);
                for (Player t : Bukkit.getOnlinePlayers()) t.playSound(t.getLocation(), s, vol, pit);
            } catch (IllegalArgumentException ignored) {}
        }

        // Feedback (chat + actionbar)
        if (charged) {
            sendDeducted(p, cost);
            sendActionbarDeducted(p, cost);
        } else if (cost <= 0.0 || bypassCost) {
            String bypassMsg = cfg.getString("megaphone.messages.bypass", "&7비용이 차감되지 않았습니다.");
            p.sendMessage(TextUtil.colorize(bypassMsg, true, true));
        }

        manager.stamp(p.getUniqueId());
        manager.setLastMsg(p.getUniqueId(), raw);
        return true;
    }

    private void sendInsufficient(Player p, double amount) {
        FileConfiguration cfg = plugin.getConfig();
        String pat = cfg.getString("megaphone.messages.insufficient", "&c잔액이 부족합니다. 필요 금액: &e{amount}");
        p.sendMessage(TextUtil.colorize(pat.replace("{amount}", format(amount)), true, true));
    }

    private void sendDeducted(Player p, double amount) {
        FileConfiguration cfg = plugin.getConfig();
        String pat = cfg.getString("megaphone.messages.deducted", "&e확성기 사용료 &6{amount}&e가 차감되었습니다.");
        p.sendMessage(TextUtil.colorize(pat.replace("{amount}", format(amount)), true, true));
    }

    private void sendActionbarDeducted(Player p, double amount) {
        FileConfiguration cfg = plugin.getConfig();
        String pat = cfg.getString("megaphone.actionbar.deducted", "&6-{amount} &e확성기");
        String s = TextUtil.colorize(pat.replace("{amount}", format(amount)), true, true);
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(s));
    }

    private String format(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long)Math.rint(v));
        return new DecimalFormat("#,##0.##").format(v);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
