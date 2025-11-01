package com.github.pixeldungeon.model;
import org.bukkit.Location;
public class Region{ public String world; public int x1,y1,z1,x2,y2,z2;
  public Region(){}
  public Region(Location a, Location b){ world=a.getWorld().getName();
    x1=Math.min(a.getBlockX(),b.getBlockX()); y1=Math.min(a.getBlockY(),b.getBlockY()); z1=Math.min(a.getBlockZ(),b.getBlockZ());
    x2=Math.max(a.getBlockX(),b.getBlockX()); y2=Math.max(a.getBlockY(),b.getBlockY()); z2=Math.max(a.getBlockZ(),b.getBlockZ());
  }
  public boolean contains(Location l){ if(!l.getWorld().getName().equals(world)) return false; int x=l.getBlockX(),y=l.getBlockY(),z=l.getBlockZ(); return x>=x1&&x<=x2 && y>=y1&&y<=y2 && z>=z1&&z<=z2; }
}