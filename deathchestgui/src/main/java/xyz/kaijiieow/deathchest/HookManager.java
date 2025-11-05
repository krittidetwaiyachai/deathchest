package xyz.kaijiieow.deathchest;

import org.bukkit.entity.Player;

// --- API จริงจาก Doc ---
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

// จัดการ API Hooks
public class HookManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final LoggingService logger;
    
    private Currency currency; // ตัวแปรสกุลเงินจริง

    public HookManager(DeathChestPlugin plugin, ConfigManager configManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่พบปลั๊กอิน CoinsEngine!");
            return false;
        }
        
        try {
            // ดึงชื่อสกุลเงินจาก Config
            String currencyName = configManager.getCurrencyName();
            
            // ดึง Object สกุลเงินจาก API
            this.currency = CoinsEngineAPI.getCurrency(currencyName);

            if (this.currency == null) {
                logger.log(LoggingService.LogLevel.ERROR, "ไม่พบสกุลเงินชื่อ '" + currencyName + "' ใน CoinsEngine!");
                return false;
            }
            
            logger.log(LoggingService.LogLevel.INFO, "เชื่อมต่อกับ CoinsEngine สำเร็จ (สกุลเงิน: " + currencyName + ")");
            return true;
        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "เชื่อมต่อ CoinsEngine ไม่สำเร็จ: " + e.getMessage());
            return false;
        }
    }

    public boolean setupHolograms() {
        if (plugin.getServer().getPluginManager().getPlugin("DecentHolograms") == null) {
             logger.log(LoggingService.LogLevel.ERROR, "ไม่พบปลั๊กอิน DecentHolograms!");
            return false;
        }
        logger.log(LoggingService.LogLevel.INFO, "เชื่อมต่อกับ DecentHolograms");
        return true;
    }

    // --- เมธอดที่เรียก API จริง ---

    public double getBalance(Player player) {
        if (this.currency == null) {
            logger.log(LoggingService.LogLevel.WARN, "เรียก getBalance ไม่ได้เพราะ currency เป็น null");
            return 0.0;
        }
        return CoinsEngineAPI.getBalance(player, this.currency);
    }

    public void withdrawMoney(Player player, double amount) {
        if (this.currency == null) {
            logger.log(LoggingService.LogLevel.WARN, "เรียก withdrawMoney ไม่ได้เพราะ currency เป็น null");
            return;
        }
        
        // API Doc ใช้ removeBalance
        CoinsEngineAPI.removeBalance(player, this.currency, amount);
        logger.log(LoggingService.LogLevel.INFO, "หักเงิน (จริง) " + amount + " " + this.currency.getId() + " จาก " + player.getName());
    }
}