package com.github.pixeldungeon.manager;
import com.github.pixeldungeon.PixelDungeonPlugin; import com.github.pixeldungeon.model.RankingEntry; import java.io.File; import java.util.*; import org.bukkit.configuration.file.YamlConfiguration;
public class RankingManager{
  private final PixelDungeonPlugin plugin;
  public RankingManager(PixelDungeonPlugin p){ this.plugin=p; }
  private File file(String dungeon){ File dir=new File(plugin.getDataFolder(), "rankings"); if(!dir.exists()) dir.mkdirs(); return new File(dir, dungeon.toLowerCase()+".yml"); }
  public java.util.List<RankingEntry> getTop(String dungeon, int top){
    java.util.List<RankingEntry> list=new java.util.ArrayList<>(); try{
      File f=file(dungeon); if(!f.exists()) return list;
      YamlConfiguration y=YamlConfiguration.loadConfiguration(f);
      java.util.List<?> arr=y.getList("entries"); if(arr!=null) for(Object o: arr){
        if(o instanceof java.util.Map){ java.util.Map m=(java.util.Map)o; RankingEntry e=new RankingEntry();
          Object names=m.get("players"); if(names instanceof java.util.List){ for(Object n:(java.util.List)names) e.players.add(String.valueOf(n)); }
          Object dur=m.get("duration"); if(dur instanceof Number) e.durationMs=((Number)dur).longValue();
          Object at=m.get("at"); if(at instanceof Number) e.at=((Number)at).longValue(); list.add(e);
        }
      }
    }catch(Exception ignored){}
    java.util.Collections.sort(list);
    if(list.size()>top) return new java.util.ArrayList<>(list.subList(0, top));
    return list;
  }
  public void record(String dungeon, java.util.List<String> names, long duration){
    java.util.List<RankingEntry> list=getTop(dungeon, Integer.MAX_VALUE);
    list.add(new RankingEntry(names, duration, System.currentTimeMillis()));
    java.util.Collections.sort(list);
    // keep at most 100
    if(list.size()>100) list=list.subList(0,100);
    save(dungeon, list);
  }
  private void save(String dungeon, java.util.List<RankingEntry> list){
    YamlConfiguration y=new YamlConfiguration();
    java.util.List<java.util.Map<String,Object>> out=new java.util.ArrayList<>();
    for(RankingEntry e: list){
      java.util.Map<String,Object> m=new java.util.LinkedHashMap<>();
      m.put("players", e.players); m.put("duration", e.durationMs); m.put("at", e.at); out.add(m);
    }
    y.set("entries", out);
    try{ y.save(file(dungeon)); }catch(Exception ignored){}
  }
}