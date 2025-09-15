package com.minkang.ultimate.usershop;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.listeners.ChatListener;
import com.minkang.ultimate.usershop.listeners.GUIListener;
import com.minkang.ultimate.usershop.listeners.InteractListener;

public class Main extends JavaPlugin {

    private static Main instance;
    private ShopManager shopManager;
    private GUIManager guiManager;
    private Economy economy;
    private boolean economyReady = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        try {
            org.bukkit.configuration.ConfigurationSection sec = getConfig().getConfigurationSection("name-map");
            java.util.Map<String,String> mmap = new java.util.LinkedHashMap<>();
            if (sec != null) {
                for (String k : sec.getKeys(false)) { mmap.put(k, String.valueOf(sec.get(k))); }
            }
            com.minkang.ultimate.usershop.util.NameMap.loadConfig(mmap);
        } catch (Throwable ignored) {}


        // managers
        shopManager = new ShopManager(this);
        guiManager = new GUIManager(this, shopManager);

        // listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this, shopManager, guiManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, guiManager), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this, guiManager), this);

        // Vault economy
        setupEconomy();

        // realtime init (safe)
        try {
            boolean enable = getConfig().getBoolean("settings.realtime_refresh.enable", true);
            int ticks = getConfig().getInt("settings.realtime_refresh.debounce_ticks", 3);
            com.minkang.ultimate.usershop.realtime.ShopRealtime.get().init(this, enable, ticks);
        } catch (Throwable ignored) {}
    }

    private void setupEconomy() {
        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                getLogger().warning("[UserShop] Economy provider not found. Economy disabled.");
                economyReady = false;
                return;
            }
            economy = rsp.getProvider();
            economyReady = economy != null;
        } catch (Throwable t) {
            getLogger().warning("[UserShop] Economy setup failed: " + t.getMessage());
            economyReady = false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("유저상점")) return false;
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            // help
            p.sendMessage("§a/유저상점 열기 §7- 내 개인 상점 열기");
            p.sendMessage("§a/유저상점 검색 §7- 검색 모드");
            p.sendMessage("§a/유저상점 §7- 상점 목록");
            guiManager.openMain(p, 1);
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("열기")) { guiManager.openMain(p, 1); return true; }
        if (sub.equalsIgnoreCase("검색")) {
            guiManager.requestSearch(p);
            return true;
        }
        // fallback: 목록
        guiManager.openMain(p, 1);
        return true;
    }

    public static Main getInstance() { return instance; }
    public ShopManager getShopManager() { return shopManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public Economy getEconomy() { return economy; }
    public boolean isEconomyReady() { return economyReady; }
}
