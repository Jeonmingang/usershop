package com.github.pixeldungeon.model;
import java.util.*;
public class RankingEntry implements Comparable<RankingEntry>{
  public java.util.List<String> players = new java.util.ArrayList<>();
  public long durationMs; public long at;
  public RankingEntry(){}
  public RankingEntry(java.util.List<String> names,long dur,long at){ this.players.addAll(names); this.durationMs=dur; this.at=at; }
  public int compareTo(RankingEntry o){ return Long.compare(this.durationMs, o.durationMs); }
}