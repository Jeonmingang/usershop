package com.github.pixeldungeon.model;
public enum Mode { SOLO, PARTY;
  public static Mode from(String s){
    if(s==null) return SOLO;
    String k = s.toLowerCase().replace(" ", "").replace(",","");
    if(k.contains("파티") || k.contains("party")) return PARTY;
    return SOLO;
  }
}
