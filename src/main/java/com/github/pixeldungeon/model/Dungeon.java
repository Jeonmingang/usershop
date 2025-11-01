package com.github.pixeldungeon.model;
import org.bukkit.Bukkit; import org.bukkit.Location; import java.util.*;
public class Dungeon{
  public String name; public String difficulty; public Mode mode=Mode.SOLO; public int minPlayers=1; public int maxPlayers=1;
  public String world; public double x,y,z,yaw,pitch; public int cooldownSeconds=300;
  public java.util.List<Stage> stages=new java.util.ArrayList<>(); public java.util.Map<Integer,RewardSet> stageRewards=new java.util.HashMap<>(); public RewardSet finalRewards=new RewardSet();
  public Location spawnLocation(){ if(world==null)return null; return new Location(Bukkit.getWorld(world),x,y,z,(float)yaw,(float)pitch); }
  public void setSpawn(Location l){ world=l.getWorld().getName(); x=l.getX(); y=l.getY(); z=l.getZ(); yaw=l.getYaw(); pitch=l.getPitch(); }
}