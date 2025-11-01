package com.github.pixeldungeon.manager;
import com.github.pixeldungeon.PixelDungeonPlugin; import com.github.pixeldungeon.model.*; import com.github.pixeldungeon.util.ItemBuilder; import com.github.pixeldungeon.util.Text;
import org.bukkit.*; import org.bukkit.entity.*; import org.bukkit.event.*; import org.bukkit.event.inventory.InventoryClickEvent; import org.bukkit.event.player.AsyncPlayerChatEvent; import org.bukkit.inventory.*;
import java.util.*;
public class RewardGUIManager implements Listener{
  private final PixelDungeonPlugin plugin; private final Map<java.util.UUID,Editor> editors=new HashMap<>(); private enum Pending{NONE,ADD_CMD,SET_MONEY}
  private static class Editor{ String dungeonName; int stageIndex; Pending pending=Pending.NONE; }
  public RewardGUIManager(PixelDungeonPlugin p){this.plugin=p;}
  public void openStage(Player p,Dungeon d,int stage){ Editor ed=new Editor(); ed.dungeonName=d.name; ed.stageIndex=stage; editors.put(p.getUniqueId(),ed);
    RewardSet rs=d.stageRewards.getOrDefault(stage,new RewardSet()); Inventory inv=Bukkit.createInventory(p,27,Text.color("&d보상설정: "+d.name+" (스테이지 "+stage+")"));
    int slot=9; for(ItemStack is: rs.items){ if(slot>17) break; inv.setItem(slot++, is.clone()); }
    inv.setItem(21,new ItemBuilder(Material.LIME_DYE).name("&a손 아이템 추가").lore("&7현재 손에 든 아이템을 보상에 추가").build());
    inv.setItem(22,new ItemBuilder(Material.PAPER).name("&b명령 추가/보기").lore("&7클릭: 채팅으로 명령 추가","&7{player} 사용 가능").build());
    inv.setItem(23,new ItemBuilder(Material.GOLD_INGOT).name("&6돈 설정: &e"+rs.money).lore("&7숫자를 채팅으로 입력").build());
    inv.setItem(24,new ItemBuilder(Material.BARRIER).name("&c초기화").lore("&7아이템/명령/돈 모두 초기화").build());
    inv.setItem(26,new ItemBuilder(Material.EMERALD_BLOCK).name("&a저장/닫기").build()); p.openInventory(inv);
  }
  public void openFinal(Player p,Dungeon d){ Editor ed=new Editor(); ed.dungeonName=d.name; ed.stageIndex=-1; editors.put(p.getUniqueId(),ed);
    RewardSet rs=d.finalRewards; Inventory inv=Bukkit.createInventory(p,27,Text.color("&6클리어보상 설정: "+d.name));
    int slot=9; for(ItemStack is: rs.items){ if(slot>17) break; inv.setItem(slot++, is.clone()); }
    inv.setItem(21,new ItemBuilder(Material.LIME_DYE).name("&a손 아이템 추가").lore("&7현재 손에 든 아이템을 보상에 추가").build());
    inv.setItem(22,new ItemBuilder(Material.PAPER).name("&b명령 추가/보기").lore("&7클릭: 채팅으로 명령 추가","&7{player} 사용 가능").build());
    inv.setItem(23,new ItemBuilder(Material.GOLD_INGOT).name("&6돈 설정: &e"+rs.money).lore("&7숫자를 채팅으로 입력").build());
    inv.setItem(24,new ItemBuilder(Material.BARRIER).name("&c초기화").lore("&7아이템/명령/돈 모두 초기화").build());
    inv.setItem(26,new ItemBuilder(Material.EMERALD_BLOCK).name("&a저장/닫기").build()); p.openInventory(inv);
  }
  @EventHandler public void onClick(InventoryClickEvent e){ if(!(e.getWhoClicked() instanceof Player)) return; Player p=(Player)e.getWhoClicked();
    Editor ed=editors.get(p.getUniqueId()); if(ed==null) return; String title=e.getView().getTitle(); if(title==null||!(title.contains("보상설정")||title.contains("클리어보상 설정"))) return; e.setCancelled(true);
    ItemStack it=e.getCurrentItem(); if(it==null||it.getType()==Material.AIR) return;
    switch(it.getType()){
      case LIME_DYE: { RewardSet rs=get(ed); plugin.rewardManager().addItem(rs, p.getInventory().getItemInMainHand()); set(ed, rs); plugin.dungeonManager().save(getD(ed)); reopen(p,ed); p.sendMessage(ChatColor.GREEN+"아이템 추가 완료"); return; }
      case PAPER: { ed.pending=Pending.ADD_CMD; p.closeInventory(); p.sendMessage(ChatColor.AQUA+"콘솔 명령을 입력하세요. 예) eco give {player} 1000 (취소: '취소')"); return; }
      case GOLD_INGOT: { ed.pending=Pending.SET_MONEY; p.closeInventory(); p.sendMessage(ChatColor.GOLD+"보상 금액(숫자)을 입력하세요. 예) 1000 (취소: '취소')"); return; }
      case BARRIER: { RewardSet rs=get(ed); rs.consoleCommands.clear(); rs.items.clear(); rs.money=0.0; set(ed,rs); plugin.dungeonManager().save(getD(ed)); reopen(p,ed); p.sendMessage(ChatColor.YELLOW+"보상 초기화 완료"); return; }
      case EMERALD_BLOCK: { editors.remove(p.getUniqueId()); p.closeInventory(); p.sendMessage(ChatColor.GREEN+"보상이 저장되었습니다."); return; }
      default: { RewardSet rs=get(ed); if(rs.items.removeIf(x->x.isSimilar(it))){ set(ed,rs); plugin.dungeonManager().save(getD(ed)); reopen(p,ed); p.sendMessage(ChatColor.YELLOW+"해당 아이템 제거"); } }
    }
  }
  @EventHandler public void onChat(AsyncPlayerChatEvent e){ Player p=e.getPlayer(); Editor ed=editors.get(p.getUniqueId()); if(ed==null) return; if(ed.pending==Pending.NONE) return; e.setCancelled(true);
    String msg=e.getMessage().trim(); if(msg.equalsIgnoreCase("취소")||msg.equalsIgnoreCase("cancel")){ ed.pending=Pending.NONE; org.bukkit.Bukkit.getScheduler().runTask(com.github.pixeldungeon.PixelDungeonPlugin.get(),()->reopen(p,ed)); return; }
    if(ed.pending==Pending.ADD_CMD){ RewardSet rs=get(ed); rs.consoleCommands.add(msg); set(ed,rs); plugin.dungeonManager().save(getD(ed)); ed.pending=Pending.NONE; org.bukkit.Bukkit.getScheduler().runTask(com.github.pixeldungeon.PixelDungeonPlugin.get(),()->{ p.sendMessage(ChatColor.GREEN+"명령 보상 추가: "+msg); reopen(p,ed); }); }
    else if(ed.pending==Pending.SET_MONEY){ try{ double money=Double.parseDouble(msg); RewardSet rs=get(ed); rs.money=money; set(ed,rs); plugin.dungeonManager().save(getD(ed)); ed.pending=Pending.NONE; org.bukkit.Bukkit.getScheduler().runTask(com.github.pixeldungeon.PixelDungeonPlugin.get(),()->{ p.sendMessage(ChatColor.GOLD+"돈 보상: "+money); reopen(p,ed); }); }catch(Exception ex){ org.bukkit.Bukkit.getScheduler().runTask(com.github.pixeldungeon.PixelDungeonPlugin.get(),()->{ p.sendMessage(ChatColor.RED+"숫자를 입력하세요."); reopen(p,ed); }); } }
  }
  private void reopen(Player p,Editor ed){ Dungeon d=getD(ed); if(d==null){ p.closeInventory(); return; } if(ed.stageIndex>=1) openStage(p,d,ed.stageIndex); else openFinal(p,d); }
  private Dungeon getD(Editor ed){ return plugin.dungeonManager().get(ed.dungeonName); }
  private RewardSet get(Editor ed){ Dungeon d=getD(ed); return ed.stageIndex>=1? d.stageRewards.getOrDefault(ed.stageIndex,new RewardSet()) : d.finalRewards; }
  private void set(Editor ed, RewardSet rs){ Dungeon d=getD(ed); if(ed.stageIndex>=1) d.stageRewards.put(ed.stageIndex, rs); else d.finalRewards=rs; }
}