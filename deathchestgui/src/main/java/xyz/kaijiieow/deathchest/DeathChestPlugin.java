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
        // 1. โหลด Config
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // 1.5 โหลด Logger (ต้องมาหลัง Config)
        this.loggingService = new LoggingService(this, configManager);

        // 2. ตรวจสอบ Dependencies
        this.hookManager = new HookManager(this, configManager, loggingService); // ส่ง ConfigManager ไปด้วย
        if (!hookManager.setupEconomy() || !hookManager.setupHolograms()) {
            loggingService.log(LoggingService.LogLevel.ERROR, "!!! ปลั๊กอินไม่สามารถทำงานได้ ขาด CoinsEngine หรือ DecentHolograms !!!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. โหลดระบบจัดเก็บ
        this.storageManager = new StorageManager();

        // 4. โหลดระบบจัดการหลัก
        this.deathChestManager = new DeathChestManager(this, configManager, hookManager, storageManager, loggingService);
        this.guiManager = new GuiManager(this, configManager, hookManager, storageManager, loggingService);

        // 5. ลงทะเบียน Listeners และ Commands
        getServer().getPluginManager().registerEvents(new DeathListener(deathChestManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);
        getCommand("buyback").setExecutor(new BuybackCommand(guiManager));

        loggingService.log(LoggingService.LogLevel.INFO, "DeathChestGUI (Refactored) เปิดใช้งานแล้ว!");
    }

    @Override
    public void onDisable() {
        // StorageManager.java ยังใช้ HashMap. ถ้าอยากให้ของถาวร ต้องไปแก้ตรงนั้น
        if (loggingService != null) {
            loggingService.log(LoggingService.LogLevel.INFO, "DeathChestGUI ปิดการใช้งาน");
            loggingService.close();
        } else {
            getLogger().info("DeathChestGUI ปิดการใช้งาน");
        }
    }

    // สร้าง Getters ให้คลาสอื่นเรียกใช้ได้
    public ConfigManager getConfigManager() { return configManager; }
    public HookManager getHookManager() { return hookManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public DeathChestManager getDeathChestManager() { return deathChestManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public LoggingService getLoggingService() { return loggingService; }
}