package com.github.pixeldungeon.manager;
import org.bukkit.*; import org.bukkit.entity.Player; import org.bukkit.inventory.ItemStack; import org.bukkit.plugin.RegisteredServiceProvider; import net.milkbowl.vault.economy.Economy;
public class RewardManager{ private Economy economy;
  public RewardManager(com.github.pixeldungeon.PixelDungeonPlugin plugin){ if(Bukkit.getPluginManager().getPlugin("Vault")!=null){ RegisteredServiceProvider<Economy> rsp=Bukkit.getServicesManager().getRegistration(Economy.class); if(rsp!=null) economy=rsp.getProvider(); } }
  public void addItem(com.github.pixeldungeon.model.RewardSet rs, ItemStack item){ if(item==null||item.getType()==Material.AIR) return; rs.items.add(item.clone()); }
  public void giveMoney(Player p,double amount){ if(economy!=null&&amount>0) economy.depositPlayer(p,amount); }
}