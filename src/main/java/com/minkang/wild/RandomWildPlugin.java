package com.minkang.wild;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RandomWildPlugin extends JavaPlugin {

    private static RandomWildPlugin instance;
    private CooldownManager cooldowns;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.cooldowns = new CooldownManager(this);

        // register whisper alias executor + tab-completer
        try {
            org.bukkit.command.PluginCommand wc = getCommand("귓");
            if (wc != null) {
                WhisperAliasCommand cmdWhisper = new WhisperAliasCommand(this);
                wc.setExecutor(cmdWhisper);
                wc.setTabCompleter(cmdWhisper);
            }
        } catch (Throwable ignored) {}

        // reload command
        try {
            org.bukkit.command.PluginCommand rc = getCommand("야생랜덤리로드");
            if (rc != null) {
                rc.setExecutor(new ReloadCommand(this));
            } else {
                getLogger().warning("Command /야생랜덤리로드 not found in plugin.yml");
            }
        } catch (Throwable ignored) {}
    
        // main random wild command
        if (getCommand("야생랜덤") != null) {
            RandomWildCommand wildCmd = new RandomWildCommand(this);
            getCommand("야생랜덤").setExecutor(wildCmd);
            getCommand("야생랜덤").setTabCompleter(wildCmd);
        } else {
            getLogger().warning("Command /야생랜덤 not found in plugin.yml");
        }

        // optional: Vault economy hook
        try { VaultHook.setup(); } 
        catch (Throwable t) { getLogger().warning("Vault setup skipped: " + t.getMessage()); }

        // shout command
        ShoutManager shoutMgr = new ShoutManager();
        if (getCommand("확성기") != null) {
            ShoutCommand shout = new ShoutCommand(this, shoutMgr);
            getCommand("확성기").setExecutor(shout);
            getCommand("확성기").setTabCompleter(shout);
        } else {
            getLogger().warning("Command /확성기 not found in plugin.yml");
        }

        // listeners
        try {
            getServer().getPluginManager().registerEvents(new WildInterceptListener(this), this);
            getServer().getPluginManager().registerEvents(new CommandVisibilityFilter(), this);
            getServer().getPluginManager().registerEvents(new ShoutInterceptListener(this, shoutMgr), this);
        } catch (Throwable ignored) {}

        getLogger().info("WildRandom enabled.");
    }
    public static RandomWildPlugin getInstance() { return instance; }
    public FileConfiguration cfg() { return getConfig(); }

    public World targetWorld() {
        String w = getConfig().getString("world", "world");
        World world = Bukkit.getWorld(w); // ALWAYS use configured target world
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            getLogger().warning("Target world '" + w + "' not found; using default world.");
            world = Bukkit.getWorlds().get(0);
        }
        return world;
    }

    public CooldownManager getCooldowns() { return cooldowns; }
}
