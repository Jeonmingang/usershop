package com.github.pixeldungeon.manager;
import com.github.pixeldungeon.PixelDungeonPlugin; import org.bukkit.configuration.file.YamlConfiguration; import java.io.File; import java.util.*;
public class LocaleManager{
  private final PixelDungeonPlugin plugin; private final Map<String,String> species=new HashMap<>(), moves=new HashMap<>();
  public LocaleManager(PixelDungeonPlugin plugin){this.plugin=plugin; load("locales/species_ko.yml", species); load("locales/moves_ko.yml", moves);}
  private void load(String path, Map<String,String> target){
    try{ File out=new File(plugin.getDataFolder(), path); if(!out.getParentFile().exists()) out.getParentFile().mkdirs();
      if(!out.exists()){ java.io.InputStream in=plugin.getResource(path); if(in!=null) java.nio.file.Files.copy(in, out.toPath()); }
      YamlConfiguration y=YamlConfiguration.loadConfiguration(out);
      for(String k: y.getKeys(false)){ String v=y.getString(k); if(k!=null&&v!=null) target.put(norm(k), v); }
    }catch(Exception ignored){}
  }
  private String norm(String s){ return s==null? "" : s.toLowerCase().replace(" ","").replace("_","").replace("-",""); }
  public String mapSpecies(String input){ String k=norm(input); return species.getOrDefault(k, input); }
  public String mapMove(String input){ String k=norm(input); String v=moves.get(k); if(v!=null) return v; return input.toLowerCase().replace(" ", "_"); }
}