package com.minkang.wild;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private static Economy econ;
    public static void setup() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) econ = rsp.getProvider();
        } catch (Throwable ignored) {}
    }
    public static boolean hasEconomy() { return econ != null; }
    public static Economy economy() { return econ; }
}
