package com.github.pixeldungeon.manager;
import com.github.pixeldungeon.PixelDungeonPlugin; import com.github.pixeldungeon.model.RankingEntry; import com.github.pixeldungeon.util.Text; import com.github.pixeldungeon.util.TimeUtil;
import org.bukkit.*; import org.bukkit.configuration.file.YamlConfiguration; import org.bukkit.entity.ArmorStand; import org.bukkit.entity.EntityType; import org.bukkit.entity.Player; import org.bukkit.inventory.EquipmentSlot;
import java.io.File; import java.util.*;
public class HologramManager{
  private final PixelDungeonPlugin plugin; private final Map<String, java.util.List<java.util.UUID>> live = new HashMap<>();
  private YamlConfiguration state;
  public HologramManager(PixelDungeonPlugin p){ this.plugin=p; loadState(); Bukkit.getScheduler().runTaskLater(plugin, this::respawnAll, 20L); }
  private File stateFile(){ return new File(plugin.getDataFolder(), "holograms.yml"); }
  private void loadState(){ try{ File f=stateFile(); state = new YamlConfiguration(); if(f.exists()) state.load(f); }catch(Exception e){ state=new YamlConfiguration(); } }
  private void saveState(){ try{ state.save(stateFile()); }catch(Exception ignored){} }
  private org.bukkit.Location getLoc(String dungeon){ if(state==null) return null; String w=state.getString(dungeon+".world"); if(w==null) return null; World world=Bukkit.getWorld(w); if(world==null) return null;
    double x=state.getDouble(dungeon+".x"), y=state.getDouble(dungeon+".y"), z=state.getDouble(dungeon+".z"); return new org.bukkit.Location(world,x,y,z);
  }
  public void install(Player p, String dungeon){
    org.bukkit.Location base = p.getLocation().getBlock().getLocation();
    base.getBlock().setType(Material.BARRIER);
    state.set(dungeon+".world", base.getWorld().getName()); state.set(dungeon+".x", base.getX()); state.set(dungeon+".y", base.getY()); state.set(dungeon+".z", base.getZ()); saveState();
    refresh(dungeon);
    p.sendMessage(ChatColor.GREEN+"홀로그램 설치 완료: "+dungeon);
  }
  public void remove(String dungeon){
    // remove stands
    java.util.List<java.util.UUID> list = live.remove(dungeon.toLowerCase());
    if(list!=null){ for(java.util.UUID id: list){ org.bukkit.entity.Entity e=Bukkit.getEntity(id); if(e!=null) e.remove(); } }
    // remove barrier block (optional): do not auto-remove block to avoid grief; but we clear if still barrier
    org.bukkit.Location base = getLoc(dungeon); if(base!=null && base.getBlock().getType()==Material.BARRIER) base.getBlock().setType(Material.AIR);
    if(state!=null){ state.set(dungeon, null); saveState(); }
  }
  private void respawnAll(){
    if(state==null) return;
    for(String dungeon: state.getKeys(false)){ refresh(dungeon); }
  }
  public void refresh(String dungeon){
    // cleanup old
    java.util.List<java.util.UUID> list = live.remove(dungeon.toLowerCase());
    if(list!=null){ for(java.util.UUID id: list){ org.bukkit.entity.Entity e=Bukkit.getEntity(id); if(e!=null) e.remove(); } }
    org.bukkit.Location base = getLoc(dungeon); if(base==null) return;
    // header line + top N
    String title = plugin.getConfig().getString("ranking.hologram.title","&e&l[ 던전 랭킹 ] &f{name}").replace("{name}", dungeon);
    double headerOffset = plugin.getConfig().getDouble("ranking.hologram.headerOffset", 1.6);
    double spacing = plugin.getConfig().getDouble("ranking.hologram.spacing", 0.28);
    int topN = plugin.getConfig().getInt("ranking.top", 10);
    java.util.List<RankingEntry> top = plugin.rankingManager().getTop(dungeon, topN);
    java.util.List<java.util.UUID> spawned = new java.util.ArrayList<>();
    // spawn header
    org.bukkit.Location loc = base.clone().add(0.5, headerOffset, 0.5);
    spawned.add(spawnLine(loc, title));
    if(top.isEmpty()){
      loc = loc.clone().add(0, -spacing, 0);
      spawned.add(spawnLine(loc, plugin.getConfig().getString("ranking.hologram.none","&8(기록 없음)")));
    } else {
      int rank=1;
      for(RankingEntry e: top){
        loc = loc.clone().add(0, -spacing, 0);
        String players = String.join(", ", e.players);
        String text = plugin.getConfig().getString("ranking.hologram.line","&7#{rank} &f{players} &8- &b{time}")
                .replace("{rank}", String.valueOf(rank)).replace("{players}", players).replace("{time}", TimeUtil.formatMillis(e.durationMs));
        spawned.add(spawnLine(loc, text));
        rank++;
      }
    }
    live.put(dungeon.toLowerCase(), spawned);
  }
  private java.util.UUID spawnLine(org.bukkit.Location loc, String text){
    ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
    as.setVisible(false); as.setSmall(true); as.setMarker(true); as.setGravity(false); as.setBasePlate(false);
    as.setCanPickupItems(false); as.setCustomNameVisible(true); as.setCustomName(Text.color(text));
    // Ensure no equipment
    as.getEquipment().setItem(EquipmentSlot.HEAD, null);
    return as.getUniqueId();
  }
}