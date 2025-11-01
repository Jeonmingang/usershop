package com.github.pixeldungeon.model; import java.util.*;
public class SpawnSpec{ public String species; public int level=30; public java.util.List<String> moves=new java.util.ArrayList<>(); public int count=3;
  public SpawnSpec(){} public SpawnSpec(String s,int l,java.util.List<String> mv,int c){species=s;level=l;if(mv!=null)moves.addAll(mv);count=c;}
}