package com.github.pixeldungeon.manager;
import org.bukkit.*; import org.bukkit.entity.Player; import java.util.*;
public class PartyManager{
  public static class Party{ public java.util.UUID leader; public java.util.Set<java.util.UUID> members=new java.util.LinkedHashSet<>(); }
  public static class Invite{ public java.util.UUID inviter; public long expireAt; }
  private final java.util.Map<java.util.UUID,Party> memberToParty=new java.util.HashMap<>();
  private final java.util.Map<java.util.UUID,Invite> invites=new java.util.HashMap<>();
  private final java.util.Set<java.util.UUID> invitePermit=new java.util.HashSet<>();
  public boolean hasPermit(java.util.UUID u){return invitePermit.contains(u);}
  public void addPermit(java.util.UUID u,long dur){invitePermit.add(u); Bukkit.getScheduler().runTaskLater(com.github.pixeldungeon.PixelDungeonPlugin.get(),()->invitePermit.remove(u),dur/50);}
  public Party getParty(java.util.UUID u){return memberToParty.get(u);} public Party createParty(Player leader){Party p=new Party(); p.leader=leader.getUniqueId(); p.members.add(leader.getUniqueId()); memberToParty.put(leader.getUniqueId(),p); return p;}
  public void disband(Party p){for(java.util.UUID u:p.members) memberToParty.remove(u);}
  public boolean isLeader(Player p){Party party=getParty(p.getUniqueId()); return party!=null && party.leader.equals(p.getUniqueId());}
  public void invite(Player inviter,Player target,long exp){Invite iv=new Invite(); iv.inviter=inviter.getUniqueId(); iv.expireAt=System.currentTimeMillis()+exp; invites.put(target.getUniqueId(),iv);
    Bukkit.getScheduler().runTaskLater(com.github.pixeldungeon.PixelDungeonPlugin.get(),()->{Invite i=invites.get(target.getUniqueId()); if(i!=null&&i.expireAt<=System.currentTimeMillis()) invites.remove(target.getUniqueId());},exp/50);}
  public Invite getInvite(java.util.UUID t){Invite iv=invites.get(t); if(iv==null) return null; if(iv.expireAt<System.currentTimeMillis()){invites.remove(t); return null;} return iv;}
  public boolean accept(Player t){Invite iv=getInvite(t.getUniqueId()); if(iv==null) return false; Player inviter=Bukkit.getPlayer(iv.inviter); if(inviter==null){invites.remove(t.getUniqueId()); return false;}
    Party p=getParty(inviter.getUniqueId()); if(p==null) p=createParty(inviter); p.members.add(t.getUniqueId()); memberToParty.put(t.getUniqueId(),p); invites.remove(t.getUniqueId()); return true;}
  public boolean deny(Player t){return invites.remove(t.getUniqueId())!=null;}
  public boolean leave(Player p){Party party=getParty(p.getUniqueId()); if(party==null) return false; party.members.remove(p.getUniqueId()); memberToParty.remove(p.getUniqueId());
    if(party.leader.equals(p.getUniqueId())){ if(!party.members.isEmpty()) party.leader=party.members.iterator().next(); else disband(party);} return true;}
  public boolean promote(Player leader,Player target){Party party=getParty(leader.getUniqueId()); if(party==null||!party.leader.equals(leader.getUniqueId())) return false; if(!party.members.contains(target.getUniqueId())) return false; party.leader=target.getUniqueId(); return true;}
  public java.util.Set<java.util.UUID> members(java.util.UUID u){Party p=getParty(u); return p==null? java.util.Collections.emptySet() : new java.util.LinkedHashSet<>(p.members);}
}