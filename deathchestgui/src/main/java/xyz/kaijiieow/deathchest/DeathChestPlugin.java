package xyz.kaijiieow.deathchest;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.List; // [FIX] Import

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
        
        // [FIX 2.1] สั่งเริ่ม Global Timer
        this.deathChestManager.startGlobalTimer();
        
        // [FIX] แก้ชื่อเมธอดให้ถูก (จาก Error ที่แล้ว)
        this.storageManager.loadBuybackItemsFromDatabase(); 

        // [FIX] ลบโค้ด 'runTaskLater' เก่าของมึงทิ้ง
        // new org.bukkit.scheduler.BukkitRunnable() { ... }.runTaskLater(this, 120L); // <--- ลบ
        
        // [FIX] ใช้โค้ด Async + Stagger ที่กูให้ไป (Phase 1.4 & 4.1)
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                // 1. โหลดข้อมูลจาก DB (Async)
                List<DatabaseChestData> dbChests = databaseManager.loadAllActiveChests();
                
                // 2. ส่งข้อมูลกลับไป Main Thread (Sync)
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        // [FIX 4.1] เรียกเมธอด Stagger (ทยอยโหลด) ให้ถูก!!
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
        // [FIX] แก้ constructor ของ AdminChestCommand
        getCommand("dctp").setExecutor(new AdminChestCommand(guiManager, configManager, loggingService, deathChestManager)); 

        loggingService.log(LoggingService.LogLevel.INFO, "DeathChestGUI (Refactored) เปิดใช้งานแล้ว!");
    }

    @Override
    public void onDisable() {
        
        // --- [FIX] โค้ด onDisable ที่แก้แล้ว (กัน Error) ---
        
        // 1. บอก Logger ให้อยู่ในโหมดปิดตัวก่อน (ห้ามรัน Async)
        if (loggingService != null) {
            loggingService.setDisabling();
        }

        if (deathChestManager != null && loggingService != null) {
            
            // 2. เซฟเวลาก่อน (สำคัญสุด)
            getLogger().info("กำลังเซฟเวลาที่เหลือของกล่องศพ (Batch)...");
            deathChestManager.saveAllChestTimes(); // <--- ตอนนี้มันจะ Log แบบ Sync
            
            // 3. ลบ entities (holograms) ทิ้ง
            getLogger().info("กำลังล้าง entities (holograms)...");
            deathChestManager.cleanupEntitiesOnDisable(); 
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // 4. ปิด Logger (File handler)
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
    public DatabaseManager getDatabaseManager() { return databaseManager; } 
}