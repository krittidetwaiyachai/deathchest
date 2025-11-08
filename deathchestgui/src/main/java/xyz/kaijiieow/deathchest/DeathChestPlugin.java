package xyz.kaijiieow.deathchest;

import org.bukkit.plugin.java.JavaPlugin;

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
        
        // [NEW] Initialize and connect DatabaseManager
        this.databaseManager = new DatabaseManager(this, configManager, loggingService);
        // [FIX] ห่อ connect() ด้วย try-catch ไม่งั้นมันไม่ log error ตอน UnsatisfiedLinkError
        try {
            this.databaseManager.connect();
        } catch (Exception e) {
            // Error นี้จะถูก log ไปแล้วใน connect() แต่เราต้องหยุด onEnable ไม่ให้ทำงานต่อ
            loggingService.log(LoggingService.LogLevel.ERROR, "!!! เกิดปัญหาหนักตอนเชื่อมต่อ Database ปลั๊กอินจะปิดตัวลง !!!");
            e.printStackTrace(); // พิมพ์ stack trace เต็มๆ ให้เห็น
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        // [FIX] ส่ง DatabaseManager กับ Logger เข้าไปดิ
        this.storageManager = new StorageManager(databaseManager, loggingService);

        // [FIX] ส่ง DatabaseManager เข้าไปด้วย
        this.deathChestManager = new DeathChestManager(this, configManager, storageManager, loggingService, databaseManager);
        
        this.guiManager = new GuiManager(this, configManager, hookManager, storageManager, loggingService);
        
        // --- [FIX] สั่งโหลดข้อมูลจาก DB ตอนเปิดเซิร์ฟ ---
        this.storageManager.loadBuybackItemsFromDatabase();
        this.deathChestManager.loadActiveChestsFromDatabase();
        // -------------------------------------------

        // --- ลงทะเบียน Listeners ---
        getServer().getPluginManager().registerEvents(new DeathListener(deathChestManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new ChestInteractListener(this, deathChestManager, configManager), this);
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(deathChestManager), this); 
        
        // --- ลงทะเบียน Commands ---
        getCommand("buyback").setExecutor(new BuybackCommand(guiManager));
        getCommand("tpchest").setExecutor(new TeleportChestCommand(deathChestManager, configManager)); 
        getCommand("dctp").setExecutor(new AdminChestCommand(guiManager, configManager, loggingService)); 

        loggingService.log(LoggingService.LogLevel.INFO, "DeathChestGUI (Refactored) เปิดใช้งานแล้ว!");
    }

    @Override
    public void onDisable() {
        // [FIX] ลบการเรียก cleanupAllChests() ทิ้ง
        // บรรทัดนี้คือตัวการที่ทำให้ของหายตอนรีเซิร์ฟ
        // if (deathChestManager != null) {
        //     deathChestManager.cleanupAllChests();
        // }
        
        // [NEW] Close database connection
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

    // --- Getters ---
    public ConfigManager getConfigManager() { return configManager; }
    public HookManager getHookManager() { return hookManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public DeathChestManager getDeathChestManager() { return deathChestManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public LoggingService getLoggingService() { return loggingService; }
    public DatabaseManager getDatabaseManager() { return databaseManager; } // [NEW]
}