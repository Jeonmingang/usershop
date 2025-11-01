package com.github.pixeldungeon.manager;

import com.github.pixeldungeon.PixelDungeonPlugin;
import com.github.pixeldungeon.model.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DungeonManager {
    private final PixelDungeonPlugin plugin;
    private final Map<String, Dungeon> dungeons = new LinkedHashMap<>();
    public DungeonManager(PixelDungeonPlugin plugin){ this.plugin=plugin; loadAll(); }
    public Collection<Dungeon> all(){ return dungeons.values(); }
    public Dungeon get(String name){ return dungeons.get(name.toLowerCase()); }

    private static int asInt(Object o, int def){ if(o instanceof Number) return ((Number)o).intValue(); try{ return Integer.parseInt(String.valueOf(o)); }catch(Exception e){ return def; } }
    private static String asStr(Object o, String def){ if(o==null) return def; String s=String.valueOf(o); return (s==null||s.equals("null"))? def : s; }

    @SuppressWarnings("unchecked")
    public void loadAll(){
        dungeons.clear();
        File folder=new File(plugin.getDataFolder(),"dungeons"); if(!folder.exists()) folder.mkdirs();
        File[] files=folder.listFiles((dir,name)->name.endsWith(".yml")); if(files==null) return;
        for(File f: files){
            try{
                YamlConfiguration y=new YamlConfiguration(); y.load(f);
                Dungeon d=new Dungeon(); d.name=y.getString("name"); if(d.name==null) continue;
                d.difficulty=y.getString("difficulty","Normal"); d.mode=Mode.valueOf(y.getString("mode","SOLO"));
                d.minPlayers=y.getInt("minPlayers",1); d.maxPlayers=y.getInt("maxPlayers", Math.max(1,d.minPlayers)); d.cooldownSeconds=y.getInt("cooldownSeconds",300);
                if(y.contains("spawn.world")){ d.world=y.getString("spawn.world"); d.x=y.getDouble("spawn.x"); d.y=y.getDouble("spawn.y"); d.z=y.getDouble("spawn.z"); d.yaw=(float)y.getDouble("spawn.yaw"); d.pitch=(float)y.getDouble("spawn.pitch"); }

                List<Map<?,?>> stages = y.getMapList("stages");
                for(Map<?,?> m : stages){
                    Stage s=new Stage();
                    s.index = asInt(m.get("index"), d.stages.size()+1);
                    if(m.containsKey("region")){
                        Map<?,?> r=(Map<?,?>)m.get("region");
                        Region R=new Region();
                        R.world=asStr(r.get("world"), d.world);
                        R.x1=asInt(r.get("x1"),0); R.y1=asInt(r.get("y1"),0); R.z1=asInt(r.get("z1"),0);
                        R.x2=asInt(r.get("x2"),0); R.y2=asInt(r.get("y2"),0); R.z2=asInt(r.get("z2"),0);
                        s.region=R;
                    }
                    // legacy objective mapping (ignored but kept for backward compat)
                    String legacy = asStr(m.get("objective"), "COMBINED").toUpperCase();
                    if("TIME".equals(legacy)){ s.killAll=false; s.timeEnabled=true; }
                    else if("KILL_TAGGED".equals(legacy)){ s.killAll=true; s.timeEnabled=false; }
                    else { s.killAll=true; s.timeEnabled=true; }

                    // new direct fields
                    Object killAll = m.get("killAll"); if(killAll!=null) s.killAll = Boolean.parseBoolean(String.valueOf(killAll));
                    Object timeEnabled = m.get("timeEnabled"); if(timeEnabled!=null) s.timeEnabled = Boolean.parseBoolean(String.valueOf(timeEnabled));

                    // time
                    if(m.containsKey("timeSeconds")) s.timeSeconds = asInt(m.get("timeSeconds"), s.timeSeconds);
                    Map<?,?> timeObj = (Map<?,?>) m.get("time");
                    if(timeObj!=null){
                        Object en=timeObj.get("enabled"); if(en!=null) s.timeEnabled = Boolean.parseBoolean(String.valueOf(en));
                        Object sec=timeObj.get("seconds"); if(sec!=null) s.timeSeconds = asInt(sec, s.timeSeconds);
                    }

                    // spawns
                    List<Map<?,?>> spList = java.util.Collections.emptyList();
                    Object spRaw = m.get("spawns");
                    if(spRaw instanceof java.util.List){
                        spList = (java.util.List<Map<?,?>>) spRaw;
                    }
                    for(Map<?,?> sm : spList){
                        SpawnSpec spc=new SpawnSpec();
                        spc.species=asStr(sm.get("species"),"Pikachu");
                        spc.level=asInt(sm.get("level"),30);
                        spc.count=asInt(sm.get("count"),3);
                        Object mv = sm.get("moves");
                        if(mv instanceof List) for(Object o : (List<?>)mv) if(o!=null) spc.moves.add(String.valueOf(o));
                        s.spawns.add(spc);
                    }
                    d.stages.add(s);
                }

                // rewards
                if(y.getConfigurationSection("stageRewards")!=null){
                    for(String key : y.getConfigurationSection("stageRewards").getKeys(false)){
                        int idx; try{ idx=Integer.parseInt(key);}catch(Exception ex){ continue; }
                        RewardSet rs=new RewardSet();
                        rs.consoleCommands = y.getStringList("stageRewards."+key+".consoleCommands");
                        rs.money = y.getDouble("stageRewards."+key+".money", 0.0);
                        List<?> list = y.getList("stageRewards."+key+".items");
                        if(list!=null) for(Object o:list) if(o instanceof ItemStack) rs.items.add(((ItemStack)o).clone());
                        d.stageRewards.put(idx, rs);
                    }
                }
                if(y.getConfigurationSection("finalRewards")!=null){
                    RewardSet rs=new RewardSet();
                    rs.consoleCommands = y.getStringList("finalRewards.consoleCommands");
                    rs.money = y.getDouble("finalRewards.money", 0.0);
                    List<?> list = y.getList("finalRewards.items");
                    if(list!=null) for(Object o:list) if(o instanceof ItemStack) rs.items.add(((ItemStack)o).clone());
                    d.finalRewards=rs;
                }

                dungeons.put(d.name.toLowerCase(), d);
            }catch(Exception ex){ plugin.getLogger().warning("Load fail: "+f.getName()+" : "+ex.getMessage()); }
        }
        plugin.getLogger().info("Loaded dungeons: "+dungeons.size());
    }

    public void save(Dungeon d){
        File folder=new File(plugin.getDataFolder(),"dungeons"); if(!folder.exists()) folder.mkdirs();
        YamlConfiguration y=new YamlConfiguration();
        y.set("name", d.name); y.set("difficulty", d.difficulty); y.set("mode", d.mode.name());
        y.set("minPlayers", d.minPlayers); y.set("maxPlayers", d.maxPlayers); y.set("cooldownSeconds", d.cooldownSeconds);
        if(d.world!=null){ y.set("spawn.world", d.world); y.set("spawn.x", d.x); y.set("spawn.y", d.y); y.set("spawn.z", d.z); y.set("spawn.yaw", d.yaw); y.set("spawn.pitch", d.pitch); }

        List<Map<String,Object>> stages=new ArrayList<>();
        for(Stage s: d.stages){
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("index", s.index);
            if(s.region!=null){
                Map<String,Object> r=new LinkedHashMap<>();
                r.put("world", s.region.world); r.put("x1", s.region.x1); r.put("y1", s.region.y1); r.put("z1", s.region.z1);
                r.put("x2", s.region.x2); r.put("y2", s.region.y2); r.put("z2", s.region.z2);
                m.put("region", r);
            }
            m.put("killAll", s.killAll);
            Map<String,Object> t=new LinkedHashMap<>(); t.put("enabled", s.timeEnabled); t.put("seconds", s.timeSeconds);
            m.put("time", t);
            List<Map<String,Object>> sp=new ArrayList<>();
            for(SpawnSpec spc: s.spawns){
                Map<String,Object> sm=new LinkedHashMap<>();
                sm.put("species", spc.species); sm.put("level", spc.level); sm.put("count", spc.count);
                sm.put("moves", spc.moves);
                sp.add(sm);
            }
            m.put("spawns", sp);
            stages.add(m);
        }
        y.set("stages", stages);

        Map<String,Object> sr=new LinkedHashMap<>();
        for(Map.Entry<Integer,RewardSet> e: d.stageRewards.entrySet()){
            Map<String,Object> rm=new LinkedHashMap<>();
            rm.put("consoleCommands", e.getValue().consoleCommands);
            rm.put("money", e.getValue().money);
            rm.put("items", e.getValue().items);
            sr.put(String.valueOf(e.getKey()), rm);
        }
        y.set("stageRewards", sr);

        Map<String,Object> fr=new LinkedHashMap<>();
        fr.put("consoleCommands", d.finalRewards.consoleCommands); fr.put("money", d.finalRewards.money); fr.put("items", d.finalRewards.items);
        y.set("finalRewards", fr);

        try{ y.save(new File(folder, d.name+".yml")); }catch(IOException e){ plugin.getLogger().warning("Save fail: "+d.name); }
    }

    public Dungeon create(String name, String diff, Mode mode, int minPlayers, Integer maxPlayers){
        Dungeon d=new Dungeon(); d.name=name; d.difficulty=diff; d.mode=mode; d.minPlayers=minPlayers; d.maxPlayers=(mode==Mode.PARTY)?(maxPlayers==null?Math.max(1,minPlayers):Math.max(minPlayers,maxPlayers)):1;
        d.cooldownSeconds=plugin.getConfig().getInt("defaults.cooldownSeconds",300);
        dungeons.put(name.toLowerCase(), d); save(d); return d;
    }
    public boolean delete(String name){
        Dungeon d=dungeons.remove(name.toLowerCase()); if(d==null) return false;
        File f=new File(new File(plugin.getDataFolder(),"dungeons"), d.name+".yml"); if(f.exists()) f.delete();
        return true;
    }
}
