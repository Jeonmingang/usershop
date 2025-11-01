package com.github.pixeldungeon.manager;
import com.github.pixeldungeon.PixelDungeonPlugin; import com.github.pixeldungeon.model.Dungeon; import com.github.pixeldungeon.util.ItemBuilder; import com.github.pixeldungeon.util.Text;
import org.bukkit.*; import org.bukkit.entity.*; import org.bukkit.event.*; import org.bukkit.event.inventory.*; import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
public class GUIManager implements Listener{
  private final PixelDungeonPlugin plugin; public GUIManager(PixelDungeonPlugin p){this.plugin=p;}
  public void openDungeonList(Player p){ int size=Math.max(9, ((plugin.dungeonManager().all().size()-1)/9+1)*9); Inventory inv=Bukkit.createInventory(p,size,Text.color("&9&l던전 목록")); int i=0;
    for(Dungeon d: plugin.dungeonManager().all()){ String ppl=d.mode.name().equals("PARTY")? (d.minPlayers+"~"+d.maxPlayers):"솔로";
      inv.setItem(i++, new ItemBuilder(Material.PAPER).name("&b"+d.name+" &7("+d.difficulty+")").lore("&7모드: &f"+d.mode,"&7인원: &f"+ppl,"&7스테이지: &f"+d.stages.size(),"&a클릭하여 참여").build()); }
    p.openInventory(inv); }
  @EventHandler public void onClick(InventoryClickEvent e){ if(e.getView().getTitle()==null||!e.getView().getTitle().contains("던전 목록")) return; e.setCancelled(true);
    ItemStack it=e.getCurrentItem(); if(it==null||it.getType()==Material.AIR) return; ItemMeta meta=it.getItemMeta(); if(meta==null||meta.getDisplayName()==null) return;
    String name=ChatColor.stripColor(meta.getDisplayName()).split(" ")[0]; Dungeon d=plugin.dungeonManager().get(name); if(d==null) return; if(e.getWhoClicked() instanceof Player){ Player p=(Player)e.getWhoClicked(); plugin.instanceManager().tryJoinAndStart(p,d); p.closeInventory(); } }
}