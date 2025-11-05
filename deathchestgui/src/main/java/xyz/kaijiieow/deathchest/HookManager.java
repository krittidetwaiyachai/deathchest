    package xyz.kaijiieow.deathchest;

    import org.bukkit.entity.Player;
    import su.nightexpress.coinsengine.api.CoinsEngineAPI;
    import su.nightexpress.coinsengine.api.currency.Currency;

    public class HookManager {

        private final DeathChestPlugin plugin;
        private final ConfigManager configManager;
        private final LoggingService logger;
        
        private Currency currency;

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
                String currencyName = configManager.getCurrencyName();
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
            
            CoinsEngineAPI.removeBalance(player, this.currency, amount);
            logger.log(LoggingService.LogLevel.INFO, "หักเงิน (จริง) " + amount + " " + this.currency.getId() + " จาก " + player.getName());
        }
    }