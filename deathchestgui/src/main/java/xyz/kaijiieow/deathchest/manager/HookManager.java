package xyz.kaijiieow.deathchest.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.util.LoggingService;

public class HookManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final LoggingService logger;

    private enum Provider { NONE, VAULT, COINS_ENGINE }
    private Provider activeProvider = Provider.NONE;

    private Economy vaultEconomy = null;
    private Currency coinsEngineCurrency;

    public HookManager(DeathChestPlugin plugin, ConfigManager configManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
    }

    public boolean setupEconomy() {
        String providerChoice = configManager.getEconomyProvider();

        if (providerChoice.equals("VAULT")) {
            if (setupVault()) {
                logger.log(LoggingService.LogLevel.INFO, "เชื่อมต่อกับ Vault สำเร็จ!");
                return true;
            } else {
                logger.log(LoggingService.LogLevel.WARN, "ตั้งค่าให้ใช้ Vault แต่ไม่พบปลั๊กอิน Vault กำลังลอง CoinsEngine...");
            }
        }
        
        if (providerChoice.equals("COINS_ENGINE") || activeProvider == Provider.NONE) {
             if (setupCoinsEngine()) {
                logger.log(LoggingService.LogLevel.INFO, "เชื่อมต่อกับ CoinsEngine สำเร็จ (สกุลเงิน: " + configManager.getCurrencyName() + ")");
                return true;
            }
        }
        
        logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถเชื่อมต่อกับระบบเศรษฐกิจใดได้ (Vault หรือ CoinsEngine)");
        return false;
    }

    private boolean setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        vaultEconomy = rsp.getProvider();
        if (vaultEconomy != null) {
            activeProvider = Provider.VAULT;
            return true;
        }
        return false;
    }

    private boolean setupCoinsEngine() {
        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
            return false;
        }
        
        try {
            String currencyName = configManager.getCurrencyName();
            this.coinsEngineCurrency = CoinsEngineAPI.getCurrency(currencyName);

            if (this.coinsEngineCurrency == null) {
                logger.log(LoggingService.LogLevel.ERROR, "ไม่พบสกุลเงินชื่อ '" + currencyName + "' ใน CoinsEngine!");
                return false;
            }
            
            activeProvider = Provider.COINS_ENGINE;
            return true;
        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "เชื่อมต่อ CoinsEngine ไม่สำเร็จ: " + e.getMessage());
            return false;
        }
    }

    public double getBalance(Player player) {
        switch (activeProvider) {
            case COINS_ENGINE:
                return CoinsEngineAPI.getBalance(player, this.coinsEngineCurrency);
            case VAULT:
                return vaultEconomy.getBalance(player);
            default:
                logger.log(LoggingService.LogLevel.WARN, "เรียก getBalance ไม่ได้เพราะไม่มีระบบเศรษฐกิจเชื่อมต่ออยู่");
                return 0.0;
        }
    }

    public void withdrawMoney(Player player, double amount) {
        switch (activeProvider) {
            case COINS_ENGINE:
                CoinsEngineAPI.removeBalance(player, this.coinsEngineCurrency, amount);
                break;
            case VAULT:
                vaultEconomy.withdrawPlayer(player, amount);
                break;
            default:
                logger.log(LoggingService.LogLevel.WARN, "เรียก withdrawMoney ไม่ได้เพราะไม่มีระบบเศรษฐกิจเชื่อมต่ออยู่");
                break;
        }
    }
    
    public String getActiveCurrencyName() {
         switch (activeProvider) {
            case COINS_ENGINE:
                return configManager.getCurrencyName();
            case VAULT:
                return vaultEconomy.currencyNamePlural();
            default:
                return "???";
        }
    }
}

