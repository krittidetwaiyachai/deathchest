package xyz.kaijiieow.deathchest;

import org.bukkit.plugin.java.JavaPlugin;

public class DeathChestPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private HookManager hookManager;
    private StorageManager storageManager;
    private DeathChestManager deathChestManager;
    private GuiManager guiManager;
    private LoggingService loggingService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        this.loggingService = new LoggingService(this, configManager);

        this.hookManager = new HookManager(this, configManager, loggingService);
        if (!hookManager.setupEconomy()) {
            loggingService.log(LoggingService.LogLevel.ERROR, "!!! ปลั๊กอินไม่สามารถทำงานได้ ขาด CoinsEngine !!!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.storageManager = new StorageManager();

        this.deathChestManager = new DeathChestManager(this, configManager, storageManager, loggingService);
        this.guiManager = new GuiManager(this, configManager, hookManager, storageManager, loggingService);

        // --- ลงทะเบียน Listeners ---
        getServer().getPluginManager().registerEvents(new DeathListener(deathChestManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new ChestInteractListener(this, deathChestManager, configManager), this);
        // [FIX] มึงลืมลงทะเบียนตัวกันระเบิด
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(deathChestManager), this); 
        
        // --- ลงทะเบียน Commands ---
        getCommand("buyback").setExecutor(new BuybackCommand(guiManager));
        // [FIX] มึงลืมลงทะเบียนคำสั่งนี้!!
        getCommand("tpchest").setExecutor(new TeleportChestCommand(deathChestManager, configManager)); 

        loggingService.log(LoggingService.LogLevel.INFO, "DeathChestGUI (Refactored) เปิดใช้งานแล้ว! (โหมดโฮโลแกรมทำเอง)");
    }

    @Override
    public void onDisable() {
        if (deathChestManager != null) {
            deathChestManager.cleanupAllChests();
        }
        
        if (loggingService != null) {
            loggingService.log(LoggingService.LogLevel.INFO, "DeathChestGUI ปิดการใช้งาน");
            loggingService.close();
        } else {
            getLogger().info("DeathChestGUI ปิดการใช้งาน");
        }
    }

    // --- Getters ---
    public ConfigManager getConfigManager() { return configManager; }
    public HookManager getHookManager() { return hookManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public DeathChestManager getDeathChestManager() { return deathChestManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public LoggingService getLoggingService() { return loggingService; }
}