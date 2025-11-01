package com.github.pixeldungeon.manager;

import com.github.pixeldungeon.PixelDungeonPlugin;
import com.github.pixeldungeon.util.Text;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public class WebhookManager {
    private final PixelDungeonPlugin plugin;
    public WebhookManager(PixelDungeonPlugin plugin){ this.plugin = plugin; }

    public boolean enabled(){
        return plugin.getConfig().getBoolean("discord.enabled", false)
                && plugin.getConfig().getString("discord.webhookUrl", "").length() > 0;
    }

    public void sendEmbed(String title, String description, int color, String dungeonName, List<String> players, String footer){
        if(!enabled()) return;
        try{
            String url = plugin.getConfig().getString("discord.webhookUrl");
            String username = plugin.getConfig().getString("discord.username","PixelDungeon");
            String avatar = plugin.getConfig().getString("discord.avatarUrl","");
            String playersStr = players==null? "-" : String.join(", ", players);
            String ts = Instant.now().toString();

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"username\":").append(json(username)).append(",");
            if(avatar!=null && !avatar.isEmpty()) sb.append("\"avatar_url\":").append(json(avatar)).append(",");
            sb.append("\"embeds\":[{");
            sb.append("\"title\":").append(json(title)).append(",");
            sb.append("\"description\":").append(json(description)).append(",");
            sb.append("\"color\":").append(color).append(",");
            sb.append("\"timestamp\":").append(json(ts)).append(",");
            sb.append("\"fields\":[");
            sb.append("{\"name\":\"던전\",\"value\":").append(json(dungeonName)).append(",\"inline\":true},");
            sb.append("{\"name\":\"파티원\",\"value\":").append(json(playersStr)).append(",\"inline\":false}");
            sb.append("]");
            if(footer!=null && !footer.isEmpty()){
                sb.append(",\"footer\":{\"text\":").append(json(footer)).append("}");
            }
            sb.append("}]}");

            byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(plugin.getConfig().getInt("discord.timeoutMs", 5000));
            con.setReadTimeout(plugin.getConfig().getInt("discord.timeoutMs", 5000));
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type","application/json");
            try(OutputStream os = con.getOutputStream()){
                os.write(body);
            }
            int code = con.getResponseCode();
            if(code < 200 || code >= 300){
                plugin.getLogger().warning("Discord webhook responded: " + code);
            }
            con.disconnect();
        } catch(Exception ex){
            plugin.getLogger().warning("Discord webhook failed: " + ex.getMessage());
        }
    }

    private String json(String s){
        if(s == null) return "null";
        String esc = s.replace("\\","\\\\").replace("\"","\\\"");
        return "\"" + esc + "\"";
    }

    // Convenience presets
    public void sendJoin(String dungeon, List<String> players, boolean party){
        String title = "던전 입장";
        String desc = party? "`파티 모드`로 입장했어요." : "`솔로 모드`로 입장했어요.";
        sendEmbed(title, desc, 3447003, dungeon, players, "");
    }
    public void sendClear(String dungeon, List<String> players, long durationMs){
        String title = "던전 클리어!";
        String desc = "총 소요 시간: **" + format(durationMs) + "**";
        sendEmbed(title, desc, 3066993, dungeon, players, "");
    }
    public void sendFail(String dungeon, List<String> players, String reason){
        String title = "던전 실패";
        String desc = "이유: **" + reason + "**";
        sendEmbed(title, desc, 15158332, dungeon, players, "");
    }
    public void sendPartyFormed(List<String> players){
        String title = "파티 결성";
        String desc = "파티가 결성되었습니다.";
        sendEmbed(title, desc, 15844367, "-", players, "");
    }

    private String format(long ms){
        long m = ms/60000; long s = (ms%60000)/1000; long msr = ms%1000;
        return String.format("%02d:%02d.%03d", m, s, msr);
    }
}
