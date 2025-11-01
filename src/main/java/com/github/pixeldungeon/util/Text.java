package com.github.pixeldungeon.util;
import net.md_5.bungee.api.ChatMessageType; import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit; import org.bukkit.ChatColor; import org.bukkit.Location; import org.bukkit.entity.Player;
public class Text{
  public static String color(String s){ return ChatColor.translateAlternateColorCodes('&', s); }
  public static void action(Player p, String m){ p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(color(m))); }
  public static void title(Player p, String t, String sub, int i, int st, int o){ p.sendTitle(color(t), color(sub), i, st, o); }
  public static void broadcast(String m){ Bukkit.broadcastMessage(color(m)); }
  public static String loc(Location l){ return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ(); }
}