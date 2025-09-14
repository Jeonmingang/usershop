package com.minkang.ultimate.usershop;

import com.minkang.ultimate.usershop.commands.UserShopCommand;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.listeners.ChatListener;
import com.minkang.ultimate.usershop.listeners.GUIListener;
import com.minkang.ultimate.usershop.listeners.InteractListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

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
        shopManager = new ShopManager(this);
        guiManager = new GUIManager(this, shopManager);

        getServer().getPluginManager().registerEvents(new GUIListener(this, shopManager, guiManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, guiManager), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this, guiManager), this);

        if (getCommand("유저상점") != null) getCommand("유저상점").setExecutor(new UserShopCommand(this, shopManager, guiManager));

        setupEconomy();
    }

    private void setupEconomy() {
        if (!getConfig().getBoolean("economy.enabled", true)) { economyReady = false; return; }
        if (getServer().getPluginManager().getPlugin("Vault") == null) { economyReady = false; return; }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) { economyReady = false; return; }
        economy = rsp.getProvider();
        economyReady = economy != null;
    }

    public static Main getInstance() { return instance; }
    public ShopManager getShopManager() { return shopManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public Economy getEconomy() { return economy; }
    public boolean isEconomyReady() { return economyReady; }
}
