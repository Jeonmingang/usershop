package com.github.pixeldungeon.model;
import org.bukkit.Bukkit; import org.bukkit.entity.Player; import org.bukkit.inventory.ItemStack; import java.util.*;
public class RewardSet{ public java.util.List<String> consoleCommands=new java.util.ArrayList<>(); public java.util.List<ItemStack> items=new java.util.ArrayList<>(); public double money=0.0;
  public void give(Player p){ for(String cmd:consoleCommands) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName())); for(ItemStack is:items) p.getInventory().addItem(is.clone()); }
}