package com.minkang.ultimate.usershop;


import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.logging.Level;
import com.minkang.nbtguard.NbtSanitizer;
import com.minkang.ultimate.usershop.commands.UserShopCommand;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.listeners.ChatListener;
import com.minkang.ultimate.usershop.listeners.GuiListener;
import com.minkang.ultimate.usershop.listeners.InteractListener;
import com.minkang.ultimate.usershop.util.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    
    private NbtSanitizer nbtSanitizer;
private PacketAdapter nbtGuardAdapter;


    private static Main instance;
    private ShopManager shopManager;
    private VaultHook vault;

    @Override
    public void onEnable() {
        nbtSanitizer = new NbtSanitizer();

        
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            nbtGuardAdapter = new PacketAdapter(this, ListenerPriority.NORMAL,
                    PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT) {
                @Override
                public void onPacketSending(PacketEvent e) {
                    try {
                        if (e.getPacket().getItemListModifier().size() > 0) {
                            java.util.List<org.bukkit.inventory.ItemStack> items =
                                    e.getPacket().getItemListModifier().read(0);
                            for (int i = 0; i < items.size(); i++) {
                                items.set(i, nbtSanitizer.sanitize(items.get(i)));
                            }
                            e.getPacket().getItemListModifier().write(0, items);
                        }
                        if (e.getPacket().getItemModifier().size() > 0) {
                            org.bukkit.inventory.ItemStack item =
                                    e.getPacket().getItemModifier().read(0);
                            e.getPacket().getItemModifier().write(0,
                                    nbtSanitizer.sanitize(item));
                        }
                    } catch (Throwable t) {
                        getLogger().log(Level.WARNING, "NBTGuard sanitize failed", t);
                    }
                }
            };
            pm.addPacketListener(nbtGuardAdapter);
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Failed to register NBTGuard listener", t);
        }

        instance = this;
        saveDefaultConfig();
        // legacy translations file
        saveResource("translations.yml", false);

        this.vault = new VaultHook(this);
        this.vault.setup();

        this.shopManager = new ShopManager(this);
        this.shopManager.loadAll();
        // periodic expiry sweep
        org.bukkit.Bukkit.getScheduler().runTaskTimer(this, () -> shopManager.sweepExpired(), 20L*60, 20L*600);

        getCommand("유저상점").setExecutor(new UserShopCommand(this));
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("UltimateUserShop v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        
        if (nbtGuardAdapter != null) {
            try { ProtocolLibrary.getProtocolManager().removePacketListener(nbtGuardAdapter); }
            catch (Throwable ignore) {}
        }

        if (shopManager != null) {
            shopManager.saveAll();
        }
        getLogger().info("UltimateUserShop disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public VaultHook getVault() {
        return vault;
    }

    public String msg(String key) {
        FileConfiguration cfg = getConfig();
        String prefix = cfg.getString("messages.prefix", "");
        String path = "messages." + key;
        String v = cfg.getString(path, path);
        if (v == null) v = path;
        return color(prefix + v);
    }

    public static String color(String s) {
        return s.replace("&", "§");
    }
}
