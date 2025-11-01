package com.github.pixeldungeon.command;

import com.github.pixeldungeon.PixelDungeonPlugin;
import com.github.pixeldungeon.manager.PartyManager;
import com.github.pixeldungeon.model.*;
import com.github.pixeldungeon.util.ItemBuilder;
import com.github.pixeldungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class DungeonCommand implements CommandExecutor, TabCompleter {

    private final PixelDungeonPlugin plugin;
    private static final java.util.Map<String,String> DEFAULT_MSG = new java.util.HashMap<>();
    static {
        // Party defaults (used if config is missing/blank)
        DEFAULT_MSG.put("party.invite_sent", "{target}님에게 &a파티 초대&f를 보냈어요.");
        DEFAULT_MSG.put("party.invite_received", "{inviter}님이 파티 초대를 보냈어요! &b/던전 파티 수락");
        DEFAULT_MSG.put("party.invite_none", "&c초대가 없거나 만료되었습니다.");
        DEFAULT_MSG.put("party.no_permit", "&c파티권이 필요합니다.");
        DEFAULT_MSG.put("party.accepted_broadcast", "&a{player}님이 {leader}님의 파티에 합류했습니다!");
        DEFAULT_MSG.put("party.denied", "&e{player}님이 초대를 거절했습니다.");
        DEFAULT_MSG.put("party.left", "&e{player}님이 파티에서 나갔습니다.");
        DEFAULT_MSG.put("party.promoted", "&d{player}님이 파티장이 되었습니다.");
        DEFAULT_MSG.put("party.promote_fail", "&c파티장 위임에 실패했습니다.");
        DEFAULT_MSG.put("party.not_party", "&c파티가 아닙니다.");
        DEFAULT_MSG.put("party.info_header", "&b&l[ 파티 정보 ]");
        DEFAULT_MSG.put("party.info_leader", "&7파티장: &f{leader}");
        DEFAULT_MSG.put("party.info_members", "&7파티원: &f{members}");
        DEFAULT_MSG.put("admin.ticket_given", "&a{player}님에게 &d파티권&f x{amount} 지급 완료");
        DEFAULT_MSG.put("party.ticket_activated", "&d파티 초대권&f이 {seconds}초 동안 활성화되었습니다.");
        DEFAULT_MSG.put("party.need_party_to_join", "&c파티 던전입니다. 먼저 파티를 맺어주세요. &7/던전 파티 <플레이어>");
    }


    public DungeonCommand(PixelDungeonPlugin plugin) { this.plugin = plugin; }

    private void ok(Player p, String m){ p.sendMessage(Text.color(m)); }
    private void info(Player p, String m){ p.sendMessage(Text.color(m)); }
    private void warn(Player p, String m){ p.sendMessage(Text.color(m)); }
    private void err(Player p, String m){ p.sendMessage(Text.color(m)); }
    private String msg(String key){ String v = plugin.getConfig().getString("messages."+key, null); if (v==null || v.trim().isEmpty()) return DEFAULT_MSG.getOrDefault(key, ""); return v; }
    private String fmt(String s, Map<String,String> v){ if(s==null) return ""; String out=s; for(Map.Entry<String,String> e: v.entrySet()) out=out.replace("{"+e.getKey()+"}", e.getValue()); return out; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Player only."); return true; }
        Player p = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("도움말")) return help(p);
        if (args[0].equalsIgnoreCase("목록") || args[0].equalsIgnoreCase("gui")) { plugin.guiManager().openDungeonList(p); return true; }

        String sub = args[0].toLowerCase();
        if (args.length >= 2 && sub.equals("스테이지") && args[1].equalsIgnoreCase("추가")) sub = "스테이지추가";
        if (args.length >= 2 && sub.equals("스테이지") && args[1].equalsIgnoreCase("삭제")) sub = "스테이지삭제";
        if (args.length >= 2 && sub.equals("스폰") && args[1].equalsIgnoreCase("위치")) sub = "스폰위치";
        if (args.length >= 2 && sub.equals("스폰") && args[1].equalsIgnoreCase("설정")) sub = "스폰설정";
        if (args.length >= 2 && sub.equals("설정") && args[1].equalsIgnoreCase("클리어보상")) sub = "설정-클리어보상";

        switch (sub){
            case "생성": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 5) { info(p,"/던전 생성 <이름> <난이도> <솔로|파티> <최소인원> [최대인원]"); return true; }
                String name=args[1]; String diff=args[2]; Mode mode=Mode.from(args[3]); int min=Integer.parseInt(args[4]); Integer max=(args.length>=6)? Integer.parseInt(args[5]) : null;
                plugin.dungeonManager().create(name, diff, mode, min, max);
                ok(p,"던전 생성: "+name+(mode==Mode.PARTY? " ("+min+"~"+(max==null?min:max)+")":" (솔로)"));
                return true;
            }
            case "삭제": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 2) { info(p,"/던전 삭제 <이름>"); return true; }
                boolean ok = plugin.dungeonManager().delete(args[1]);
                if(ok) ok(p,"삭제 완료"); else err(p,"삭제 실패/미존재");
                return true;
            }
            case "스폰위치": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 2) { info(p,"/던전 스폰 위치 <이름>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                d.setSpawn(p.getLocation()); plugin.dungeonManager().save(d); ok(p,"스폰 위치 저장"); return true;
            }
            case "구역": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 3) { info(p,"/던전 구역 <이름> <스테이지>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                int idx=Integer.parseInt(args[2]); while(d.stages.size()<idx){ Stage st=new Stage(); st.index=d.stages.size()+1; d.stages.add(st); }
                if(plugin.selectionManager().getPos1(p)==null || plugin.selectionManager().getPos2(p)==null){ warn(p,"나무도끼로 좌/우클릭하여 영역을 먼저 선택하세요."); return true; }
                d.stages.get(idx-1).region=new Region(plugin.selectionManager().getPos1(p), plugin.selectionManager().getPos2(p));
                plugin.dungeonManager().save(d); ok(p,"스테이지 "+idx+" 구역 저장"); return true;
            }
            case "스테이지추가": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 3) { info(p,"/던전 스테이지 추가 <이름> <번호>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                int idx=Integer.parseInt(args[2]); Stage s=new Stage(); s.index=idx; d.stages.add(s);
                d.stages=d.stages.stream().sorted(Comparator.comparingInt(st->st.index)).collect(Collectors.toList());
                plugin.dungeonManager().save(d); ok(p,"스테이지 추가"); return true;
            }
            case "스테이지삭제": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 3) { info(p,"/던전 스테이지 삭제 <이름> <번호>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                int idx=Integer.parseInt(args[2]); d.stages.removeIf(s->s.index==idx); plugin.dungeonManager().save(d); ok(p,"스테이지 삭제"); return true;
            }
            case "목표": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 3) { info(p,"/던전 목표 <이름> <스테이지> [시간초]"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                int idx=Integer.parseInt(args[2]); while(d.stages.size()<idx){ Stage st=new Stage(); st.index=d.stages.size()+1; d.stages.add(st); }
                Stage s=d.stages.get(idx-1);
                s.killAll=true; s.timeEnabled=true;
                if(args.length>=4){ try{ s.timeSeconds=Integer.parseInt(args[3]); }catch(Exception ignored){} }
                s.objective="COMBINED";
                plugin.dungeonManager().save(d);
                ok(p,"스테이지 "+idx+" 목표=둘다, 제한 "+s.timeSeconds+"초 (미처치시 실패)");
                return true;
            }
            case "웨이브간격": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 4) { info(p,"/던전 웨이브간격 <이름> <스테이지> <초>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                int idx=Integer.parseInt(args[2]); int sec=Integer.parseInt(args[3]); while(d.stages.size()<idx){ Stage st=new Stage(); st.index=d.stages.size()+1; d.stages.add(st); }
                d.stages.get(idx-1).waveIntervalSeconds=sec; plugin.dungeonManager().save(d); ok(p,"스테이지 "+idx+" 웨이브 간격="+sec+"초"); return true;
            }
            case "스폰설정": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 7) { info(p,"/던전 스폰 설정 <이름> <스테이지> <포켓몬> <레벨> <기술1,기술2,기술3,기술4> <수량>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                int idx=Integer.parseInt(args[2]); String rawSpecies=args[3]; int level=Integer.parseInt(args[4]); String rawMoves=args[5]; int count=Integer.parseInt(args[6]);
                while(d.stages.size()<idx){ Stage st=new Stage(); st.index=d.stages.size()+1; d.stages.add(st); }
                String species=plugin.localeManager().mapSpecies(rawSpecies);
                List<String> mv=new ArrayList<>(); for(String m2: rawMoves.split("[/,，,]")){ m2=m2.trim(); if(m2.isEmpty()) continue; mv.add(plugin.localeManager().mapMove(m2)); if(mv.size()>=4) break; }
                d.stages.get(idx-1).spawns.add(new SpawnSpec(species, level, mv, count)); plugin.dungeonManager().save(d);
                ok(p,"스테이지 "+idx+" 스폰 프리셋 추가: "+species+" L"+level+" x"+count+" moves="+mv); return true;
            }
            case "보상": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 3) { info(p,"/던전 보상 <이름> <스테이지>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                int idx=Integer.parseInt(args[2]); plugin.rewardGUI().openStage(p,d,idx); return true;
            }
            case "설정-클리어보상": case "클리어보상": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                String target = sub.equals("설정-클리어보상") ? (args.length>=3? args[2]: null) : (args.length>=2? args[1]: null);
                if (target==null){ info(p,"/던전 클리어보상 <이름>"); return true; }
                Dungeon d=plugin.dungeonManager().get(target); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                plugin.rewardGUI().openFinal(p,d); return true;
            }
            case "랭킹": {
                if (args.length < 2){ info(p,"/던전 랭킹 <이름>"); return true; }
                String name=args[1]; List<RankingEntry> top=plugin.rankingManager().getTop(name, plugin.getConfig().getInt("ranking.top",10));
                if (top.isEmpty()){ info(p,"기록 없음"); return true; }
                int i=1; info(p,"&b[던전 랭킹] &f"+name);
                for(RankingEntry e: top){
                    p.sendMessage(Text.color("&7#"+i+" &f"+String.join(", ", e.players)+" &8- &b"+com.github.pixeldungeon.util.TimeUtil.formatMillis(e.durationMs)));
                    i++;
                }
                return true;
            }
            case "홀로그램": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 3){ info(p,"/던전 홀로그램 설치 <이름>  |  /던전 홀로그램 제거 <이름>"); return true; }
                String act=args[1]; String name=args[2];
                if (act.equalsIgnoreCase("설치")||act.equalsIgnoreCase("install")){ plugin.hologramManager().install(p, name); ok(p,"홀로그램 설치 완료: "+name); }
                else if (act.equalsIgnoreCase("제거")||act.equalsIgnoreCase("remove")){ plugin.hologramManager().remove(name); warn(p,"홀로그램 제거 완료"); }
                return true;
            }
            case "파티": {
                if (args.length==2 && !Arrays.asList("초대","수락","거절","탈퇴","위임","정보").contains(args[1])){
                    if (!plugin.partyManager().hasPermit(p.getUniqueId()) && !p.hasPermission("pixeldungeon.admin")){ err(p, msg("party.no_permit")); return true; }
                    Player t=Bukkit.getPlayer(args[1]); if(t==null){ err(p,"오프라인/미접속"); return true; }
                    plugin.partyManager().invite(p, t, plugin.getConfig().getInt("partyInviteExpireSeconds",60)*1000L);
                    Map<String,String> v=new HashMap<>(); v.put("target", t.getName()); info(p, fmt(msg("party.invite_sent"), v));
                    Map<String,String> v2=new HashMap<>(); v2.put("inviter", p.getName()); info(t, fmt(msg("party.invite_received"), v2));
                    return true;
                }
                if (args.length < 2){ info(p,"/던전 파티 초대 <플레이어> | 수락 | 거절 | 탈퇴 | 위임 <플레이어> | 정보"); return true; }
                String act=args[1].toLowerCase();
                if (act.equals("초대")||act.equals("invite")){
                    if (!plugin.partyManager().hasPermit(p.getUniqueId()) && !p.hasPermission("pixeldungeon.admin")){ err(p, msg("party.no_permit")); return true; }
                    if (args.length < 3){ info(p,"/던전 파티 초대 <플레이어>"); return true; }
                    Player t=Bukkit.getPlayer(args[2]); if(t==null){ err(p,"오프라인/미접속"); return true; }
                    plugin.partyManager().invite(p, t, plugin.getConfig().getInt("partyInviteExpireSeconds",60)*1000L);
                    Map<String,String> v=new HashMap<>(); v.put("target", t.getName()); info(p, fmt(msg("party.invite_sent"), v));
                    Map<String,String> v2=new HashMap<>(); v2.put("inviter", p.getName()); info(t, fmt(msg("party.invite_received"), v2));
                } else if (act.equals("수락")||act.equals("accept")){
                    boolean ok2 = plugin.partyManager().accept(p);
                    if (ok2){
                        List<String> names=new ArrayList<>(); for(UUID u2: plugin.partyManager().members(p.getUniqueId())){ Player pl=Bukkit.getPlayer(u2); if(pl!=null) names.add(pl.getName()); }
                        plugin.webhook().sendPartyFormed(names);
                        String leader = names.isEmpty()? "-" : names.get(0);
                        Map<String,String> v=new HashMap<>(); v.put("player", p.getName()); v.put("leader", leader);
                        info(p, fmt(msg("party.accepted_broadcast"), v));
                        Player lp = Bukkit.getPlayer(leader); if(lp!=null) info(lp, fmt(msg("party.accepted_broadcast"), v));
                    } else {
                        err(p, msg("party.invite_none"));
                    }
                } else if (act.equals("거절")||act.equals("deny")){
                    boolean ok3 = plugin.partyManager().deny(p);
                    if (ok3){ Map<String,String> v=new HashMap<>(); v.put("player", p.getName()); info(p, fmt(msg("party.denied"), v)); }
                    else { err(p, msg("party.invite_none")); }
                } else if (act.equals("탈퇴")||act.equals("leave")){
                    boolean ok4 = plugin.partyManager().leave(p);
                    if (ok4){ Map<String,String> v=new HashMap<>(); v.put("player", p.getName()); warn(p, fmt(msg("party.left"), v)); }
                    else { err(p, msg("party.not_party")); }
                } else if (act.equals("위임")||act.equals("promote")){
                    if (args.length < 3){ info(p,"/던전 파티 위임 <플레이어>"); return true; }
                    Player t=Bukkit.getPlayer(args[2]); if(t==null){ err(p,"대상 접속중 아님"); return true; }
                    boolean ok5 = plugin.partyManager().promote(p, t);
                    if (ok5){ Map<String,String> v=new HashMap<>(); v.put("player", t.getName()); info(p, fmt(msg("party.promoted"), v)); info(t, fmt(msg("party.promoted"), v)); }
                    else { err(p, msg("party.promote_fail")); }
                } else if (act.equals("정보")||act.equals("info")){
                    PartyManager.Party prt = plugin.partyManager().getParty(p.getUniqueId());
                    if (prt==null){ err(p, fmt(msg("party.not_party"), new HashMap<>())); return true; }
                    List<String> names=new ArrayList<>(); for(UUID u2: prt.members){ Player pl=Bukkit.getPlayer(u2); if(pl!=null) names.add(pl.getName()); }
                    String leader = Bukkit.getPlayer(prt.leader)!=null? Bukkit.getPlayer(prt.leader).getName(): "-";
                    Map<String,String> v=new HashMap<>(); v.put("leader", leader); v.put("members", String.join(", ", names));
                    info(p, fmt(msg("party.info_header"), v));
                    info(p, fmt(msg("party.info_leader"), v));
                    info(p, fmt(msg("party.info_members"), v));
                }
                return true;
            }
            case "파티권": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                String targetName=null; int amount=1;
                if (args.length>=2 && !args[1].equalsIgnoreCase("지급") && !args[1].equalsIgnoreCase("give")){
                    targetName=args[1]; if(args.length>=3) try{ amount=Integer.parseInt(args[2]); }catch(Exception ignored){}
                } else {
                    if (args.length<3){ info(p,"/던전 파티권 지급 <플레이어> <개수>  또는  /던전 파티권 <플레이어> <개수>"); return true; }
                    targetName=args[2]; if(args.length>=4) try{ amount=Integer.parseInt(args[3]); }catch(Exception ignored){}
                }
                Player t=Bukkit.getPlayer(targetName); if(t==null){ err(p,"오프라인/미접속"); return true; }
                NamespacedKey key=plugin.ticketKey(); ItemStack ticket=new ItemBuilder(Material.PAPER).name("&d[던전 파티권]").lore("&7우클릭시 초대권 활성화","&7기간: "+plugin.getConfig().getInt("partyTicketDurationSeconds")+"초").pdc(key,"1").build();
                ticket.setAmount(Math.max(1, amount)); t.getInventory().addItem(ticket);
                Map<String,String> v=new HashMap<>(); v.put("player", t.getName()); v.put("amount", String.valueOf(amount)); info(p, fmt(msg("admin.ticket_given"), v));
                return true;
            }
            case "시작": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                if (args.length < 2) { info(p,"/던전 시작 <이름>"); return true; }
                Dungeon d=plugin.dungeonManager().get(args[1]); if(d==null){ err(p,"존재하지 않는 던전"); return true; }
                plugin.instanceManager().tryJoinAndStart(p,d); return true;
            }
            case "리로드": {
                if (!p.hasPermission("pixeldungeon.admin")) { err(p,"권한이 없습니다."); return true; }
                plugin.reloadConfig(); plugin.dungeonManager().loadAll(); ok(p,"리로드 완료"); return true;
            }
            default: return help(p);
        }
    }

    private boolean help(Player p){
        p.sendMessage(Text.color("&b&l[ 던전 도움말 ]"));
        p.sendMessage(Text.color("&b/던전 목록 &7- 던전 목록 GUI"));
        p.sendMessage(Text.color("&b/던전 생성 &7<이름> <난이도> <솔로|파티> <최소>[최대]"));
        p.sendMessage(Text.color("&b/던전 스폰 위치 &7<이름>"));
        p.sendMessage(Text.color("&b/던전 스폰 설정 &7<이름> <스테이지> <포켓몬> <레벨> <기술1,2,3,4> <수량>"));
        p.sendMessage(Text.color("&b/던전 구역 &7<이름> <스테이지>"));
        p.sendMessage(Text.color("&b/던전 스테이지 추가|삭제 &7<이름> <번호>"));
        p.sendMessage(Text.color("&b/던전 목표 &7<이름> <스테이지> [시간초]"));
        p.sendMessage(Text.color("&b/던전 웨이브간격 &7<이름> <스테이지> <초>"));
        p.sendMessage(Text.color("&b/던전 보상 &7<이름> <스테이지>"));
        p.sendMessage(Text.color("&b/던전 클리어보상 &7<이름>"));
        p.sendMessage(Text.color("&b/던전 랭킹 &7<이름>"));
        p.sendMessage(Text.color("&b/던전 홀로그램 &7설치|제거 <이름>"));
        p.sendMessage(Text.color("&b/던전 파티 &7<플레이어> | 초대|수락|거절|탈퇴|위임|정보"));
        p.sendMessage(Text.color("&8(Admin) &b/던전 파티권 지급 &7<플레이어> <개수>  &b/던전 시작 &7<이름>  &b/던전 리로드"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(Arrays.asList("도움말","목록","생성","삭제","스폰","구역","스테이지","목표","웨이브간격","보상","클리어보상","랭킹","홀로그램","파티","파티권","시작","리로드"));
        } else if (args.length == 2 && Arrays.asList("삭제","스폰설정","스폰위치","구역","스테이지추가","스테이지삭제","목표","웨이브간격","보상","클리어보상","랭킹","홀로그램","시작").contains(args[0])) {
            out.addAll(plugin.dungeonManager().all().stream().map(d->d.name).collect(java.util.stream.Collectors.toList()));
        } else if (args.length==2 && args[0].equals("스테이지")) {
            out.add("추가"); out.add("삭제");
        } else if (args.length==2 && args[0].equals("스폰")) {
            out.add("설정"); out.add("위치");
        } else if (args.length==2 && args[0].equals("설정")) {
            out.add("클리어보상");
        } else if (args.length==2 && args[0].equals("파티")) {
            out.addAll(Arrays.asList("초대","수락","거절","탈퇴","위임","정보"));
        } else if (args.length==2 && args[0].equals("홀로그램")) {
            out.add("설치"); out.add("제거");
        } else if (args.length==3 && args[0].equals("구역")) {
            out.add("1"); out.add("2"); out.add("3");
        } else if (args.length==3 && args[0].equals("파티") && (args[1].equals("초대")||args[1].equalsIgnoreCase("invite")||args[1].equals("위임")||args[1].equalsIgnoreCase("promote"))) {
            for(Player op: Bukkit.getOnlinePlayers()) out.add(op.getName());
        } else if (args.length==2 && args[0].equals("파티권")) {
            for(Player op: Bukkit.getOnlinePlayers()) out.add(op.getName());
        }
        return out;
    }
}
