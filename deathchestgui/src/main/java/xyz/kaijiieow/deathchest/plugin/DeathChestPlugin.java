package xyz.kaijiieow.deathchest.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.kaijiieow.deathchest.command.AdminChestCommand;
import xyz.kaijiieow.deathchest.command.BuybackCommand;
import xyz.kaijiieow.deathchest.command.TeleportChestCommand;
import xyz.kaijiieow.deathchest.database.DatabaseManager;
import xyz.kaijiieow.deathchest.gui.GuiManager;
import xyz.kaijiieow.deathchest.listener.ChestInteractListener;
import xyz.kaijiieow.deathchest.listener.ChestProtectionListener;
import xyz.kaijiieow.deathchest.listener.DeathListener;
import xyz.kaijiieow.deathchest.listener.GuiListener;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.manager.DeathChestManager;
import xyz.kaijiieow.deathchest.manager.HookManager;
import xyz.kaijiieow.deathchest.manager.StorageManager;
import xyz.kaijiieow.deathchest.model.DatabaseChestData;
import xyz.kaijiieow.deathchest.util.LoggingService;

import java.util.List;

public class DeathChestPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private HookManager hookManager;
    private StorageManager storageManager;
    private DeathChestManager deathChestManager;
    private GuiManager guiManager;
    private LoggingService loggingService;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        this.loggingService = new LoggingService(this, configManager);

        this.hookManager = new HookManager(this, configManager, loggingService);
        if (!hookManager.setupEconomy()) {
            loggingService.log(LoggingService.LogLevel.ERROR, "!!! ไม่สามารถเชื่อมต่อกับระบบเศรษฐกิจ (Vault หรือ CoinsEngine) ได้ ปลั๊กอินจะปิดตัวลง !!!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.databaseManager = new DatabaseManager(this, configManager, loggingService);
        try {
            this.databaseManager.connect();
        } catch (Exception e) {
            loggingService.log(LoggingService.LogLevel.ERROR, "!!! เกิดปัญหาหนักตอนเชื่อมต่อ Database ปลั๊กอินจะปิดตัวลง !!!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.storageManager = new StorageManager(databaseManager, loggingService);
        this.deathChestManager = new DeathChestManager(this, configManager, storageManager, loggingService, databaseManager);
        this.guiManager = new GuiManager(this, configManager, hookManager, storageManager, loggingService);
        
        this.storageManager.loadBuybackItemsFromDatabase();

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                List<DatabaseChestData> dbChests = databaseManager.getChestDatabase().loadAllActiveChests();
                
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        deathChestManager.staggerLoadChests(dbChests);
                    }
                }.runTask(DeathChestPlugin.this);
            }
        }.runTaskAsynchronously(this);

        getServer().getPluginManager().registerEvents(new DeathListener(deathChestManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new ChestInteractListener(this, deathChestManager, configManager), this);
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(deathChestManager), this);
        
        getCommand("buyback").setExecutor(new BuybackCommand(guiManager));
        getCommand("tpchest").setExecutor(new TeleportChestCommand(deathChestManager, configManager));
        getCommand("dctp").setExecutor(new AdminChestCommand(guiManager, configManager, loggingService, deathChestManager));

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                loggingService.log(LoggingService.LogLevel.INFO, "Global Timer กำลังจะเริ่มทำงาน...");
                deathChestManager.startGlobalTimer();
            }
        }.runTaskLater(this, 100L);

        loggingService.log(LoggingService.LogLevel.INFO, "DeathChestGUI (Refactored) เปิดใช้งานแล้ว!");
    }

    @Override
    public void onDisable() {
        if (loggingService != null) {
            loggingService.setDisabling();
        }

        if (deathChestManager != null && loggingService != null) {
            getLogger().info("กำลังเซฟเวลาที่เหลือของกล่องศพ (Batch)...");
            deathChestManager.saveAllChestTimes();
            
            getLogger().info("กำลังล้าง entities (holograms)...");
            deathChestManager.cleanupEntitiesOnDisable();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        if (loggingService != null) {
            getLogger().info("DeathChestGUI ปิดการใช้งาน");
            loggingService.close();
        } else {
            getLogger().info("DeathChestGUI ปิดการใช้งาน");
        }
    }

    public ConfigManager getConfigManager() { return configManager; }
    public HookManager getHookManager() { return hookManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public DeathChestManager getDeathChestManager() { return deathChestManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public LoggingService getLoggingService() { return loggingService; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
}

