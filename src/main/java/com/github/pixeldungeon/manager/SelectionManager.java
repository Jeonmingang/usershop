package com.github.pixeldungeon.manager;
import com.github.pixeldungeon.util.Text; import org.bukkit.*; import org.bukkit.entity.Player; import org.bukkit.event.*; import org.bukkit.event.block.Action; import org.bukkit.event.player.PlayerInteractEvent; import java.util.*;
public class SelectionManager implements Listener{
  private final Map<java.util.UUID, Location> pos1=new HashMap<>(), pos2=new HashMap<>();
  public Location getPos1(Player p){return pos1.get(p.getUniqueId());} public Location getPos2(Player p){return pos2.get(p.getUniqueId());}
  @EventHandler public void onInteract(PlayerInteractEvent e){ if(!e.getPlayer().hasPermission("pixeldungeon.admin")) return; if(e.getItem()==null||e.getItem().getType()!=Material.WOODEN_AXE) return;
    if(e.getAction()==Action.LEFT_CLICK_BLOCK){ pos1.put(e.getPlayer().getUniqueId(), e.getClickedBlock().getLocation()); Text.action(e.getPlayer(),"&aPos1 선택"); e.setCancelled(true);} 
    else if(e.getAction()==Action.RIGHT_CLICK_BLOCK){ pos2.put(e.getPlayer().getUniqueId(), e.getClickedBlock().getLocation()); Text.action(e.getPlayer(),"&aPos2 선택"); e.setCancelled(true);} }
}