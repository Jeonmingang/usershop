\1
import com.minkang.ultimate.usershop.realtime.ShopRealtime;
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
    \1try { boolean enable = getConfig().getBoolean("settings.realtime_refresh.enable", true); int ticks = getConfig().getInt("settings.realtime_refresh.debounce_ticks", 3); ShopRealtime.get().init(this, enable, ticks);} catch (Throwable ignored) {}
instance = this;
        saveDefaultConfig();
        shopManager = new ShopManager(this);
        guiManager = new GUIManager(this, shopManager);

        getServer().getPluginManager().registerEvents(new GUIListener(this, shopManager, guiManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, guiManager), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this, guiManager), this);

        if (getCommand("유저상점") != null) {
            getCommand("유저상점").setExecutor(new UserShopCommand(this, shopManager, guiManager));
        }

        setupEconomy();
        getLogger().info("[UserShop] Enabled. Economy=" + (economyReady ? "OK" : "Disabled"));
    }

    @Override
    public void onDisable() {
        shopManager.saveAll();
        getLogger().info("[UserShop] Disabled.");
    }

    private void setupEconomy() {
        boolean enabledInConfig = getConfig().getBoolean("economy.enabled", true);
        if (!enabledInConfig) {
            economyReady = false;
            return;
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("[UserShop] Vault not found. Economy disabled.");
            economyReady = false;
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("[UserShop] Economy provider not found. Economy disabled.");
            economyReady = false;
            return;
        }
        economy = rsp.getProvider();
        economyReady = economy != null;
    }

    public static Main getInstance() { return instance; }
    public ShopManager getShopManager() { return shopManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public Economy getEconomy() { return economy; }
    public boolean isEconomyReady() { return economyReady; }
}