package xyz.kaijiieow.deathchest.manager;

import org.bukkit.configuration.file.FileConfiguration;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;

import java.util.List;

public class ConfigManager {

    private final DeathChestPlugin plugin;
    private int despawnTime;
    private String economyProvider;
    private String currencyName;
    private int buybackCost;
    private boolean allowOtherPlayersToOpen;
    private double hologramYOffset;
    private int tpChestCooldown;
    private List<String> hologramLines;
    private boolean showParticles;

    private String chatMessageDeath;
    private String chatMessageExpired;
    private String chatMessageNotYourChest;
    private String chatMessageXpRestored;
    private String chatMessageNoPermission;
    private String chatMessageNoChestFound;
    private String chatMessageTeleported;
    private String chatMessageTeleportCooldown;

    private String chatMessageAdminNoPlayerFound;
    private String chatMessageAdminNoChests;
    private String chatMessageAdminTeleported;
    private String chatMessageAdminIsBuyback;

    private boolean fileLoggingEnabled;
    private String logFileName;
    private boolean discordLoggingEnabled;
    private String discordWebhookUrl;
    private String discordUsername;
    
    private String dbType;
    private String dbFilename;
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUsername;
    private String dbPassword;

    public ConfigManager(DeathChestPlugin plugin) {
        this.plugin = plugin;
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();
        loadConfigValues();
    }

    private void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();
        
        this.dbType = config.getString("database.type", "SQLITE").toUpperCase();
        this.dbFilename = config.getString("database.filename", "storage.db");
        this.dbHost = config.getString("database.mysql.host", "localhost");
        this.dbPort = config.getInt("database.mysql.port", 3306);
        this.dbDatabase = config.getString("database.mysql.database", "deathchest");
        this.dbUsername = config.getString("database.mysql.username", "user");
        this.dbPassword = config.getString("database.mysql.password", "pass");

        this.economyProvider = config.getString("settings.economy-provider", "COINS_ENGINE").toUpperCase();
        
        this.despawnTime = config.getInt("settings.despawn-timer", 300);
        this.currencyName = config.getString("settings.currency-name", "coins");
        this.buybackCost = config.getInt("settings.buyback-cost", 1000);
        this.allowOtherPlayersToOpen = config.getBoolean("settings.allow-other-players-to-open", false);
        this.hologramYOffset = config.getDouble("settings.hologram-y-offset", 1.2);
        this.tpChestCooldown = config.getInt("settings.teleport-command-cooldown", 30);
        this.hologramLines = config.getStringList("settings.hologram-lines");
        this.showParticles = config.getBoolean("settings.show-particles", true);

        this.chatMessageDeath = config.getString("messages.death", "§cคุณตาย! ของของคุณอยู่ในกล่องที่ตำแหน่ง: §f%coords% §c(XP: §f%xp%§c)");
        this.chatMessageExpired = config.getString("messages.expired", "§cกล่องเก็บของของคุณหมดเวลา! §eคุณสามารถซื้อคืนได้ด้วยคำสั่ง §f/buyback");
        this.chatMessageNotYourChest = config.getString("messages.not_your_chest", "§cนี่ไม่ใช่กล่องศพของคุณ!");
        this.chatMessageXpRestored = config.getString("messages.xp_restored", "§aคุณได้รับ XP คืน: %xp%");
        this.chatMessageNoPermission = config.getString("messages.no_permission", "§cคุณไม่มียศพอที่จะใช้คำสั่งนี้!");
        this.chatMessageNoChestFound = config.getString("messages.no_chest_found", "§cไม่พบกล่องศพของคุณ");
        this.chatMessageTeleported = config.getString("messages.teleported", "§aวาร์ปไปยังกล่องศพของคุณ!");
        this.chatMessageTeleportCooldown = config.getString("messages.teleport-cooldown", "§cใจเย็น! ต้องรออีก %time% วินาทีก่อนจะวาร์ปได้");

        this.chatMessageAdminNoPlayerFound = config.getString("messages.admin-no-player-found", "§cไม่พบผู้เล่นชื่อ %player% หรือผู้เล่นนี้ยังไม่เคยเข้าเซิร์ฟเวอร์");
        this.chatMessageAdminNoChests = config.getString("messages.admin-no-chests-found", "§eผู้เล่น %player% ไม่มีกล่องศพที่กำลังใช้งานอยู่ หรือของใน Buyback");
        this.chatMessageAdminTeleported = config.getString("messages.admin-teleported", "§aวาร์ปไปยังกล่องศพ!");
        this.chatMessageAdminIsBuyback = config.getString("messages.admin-is-buyback", "§cนี่คือรายการใน Buyback ไม่สามารถวาร์ปได้");

        this.fileLoggingEnabled = config.getBoolean("logging.file.enable", true);
        this.logFileName = config.getString("logging.file.log-name", "DeathChestGUI.log");
        this.discordLoggingEnabled = config.getBoolean("logging.discord.enable", false);
        this.discordWebhookUrl = config.getString("logging.discord.webhook-url", "YOUR_WEBHOOK_URL_HERE");
        this.discordUsername = config.getString("logging.discord.username", "DeathChest Logger");
    }
    
    public String getDbType() { return dbType; }
    public String getDbFilename() { return dbFilename; }
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbDatabase() { return dbDatabase; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }

    public String getEconomyProvider() { return economyProvider; }

    public int getDespawnTime() { return despawnTime; }
    public String getCurrencyName() { return currencyName; }
    public int getBuybackCost() { return buybackCost; }
    public boolean isAllowOtherPlayersToOpen() { return allowOtherPlayersToOpen; }
    public double getHologramYOffset() { return hologramYOffset; }
    public int getTpChestCooldown() { return tpChestCooldown; }
    public List<String> getHologramLines() { return hologramLines; }
    public boolean isShowParticles() { return showParticles; }

    public String getChatMessageDeath() { return chatMessageDeath; }
    public String getChatMessageExpired() { return chatMessageExpired; }
    public String getChatMessageNotYourChest() { return chatMessageNotYourChest; }
    public String getChatMessageXpRestored() { return chatMessageXpRestored; }
    public String getChatMessageNoPermission() { return chatMessageNoPermission; }
    public String getChatMessageNoChestFound() { return chatMessageNoChestFound; }
    public String getChatMessageTeleported() { return chatMessageTeleported; }
    public String getChatMessageTeleportCooldown() { return chatMessageTeleportCooldown; }

    public String getChatMessageAdminNoPlayerFound() { return chatMessageAdminNoPlayerFound; }
    public String getChatMessageAdminNoChests() { return chatMessageAdminNoChests; }
    public String getChatMessageAdminTeleported() { return chatMessageAdminTeleported; }
    public String getChatMessageAdminIsBuyback() { return chatMessageAdminIsBuyback; }

    public boolean isFileLoggingEnabled() { return fileLoggingEnabled; }
    public String getLogFileName() { return logFileName; }
    public boolean isDiscordLoggingEnabled() { return discordLoggingEnabled; }
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public String getDiscordUsername() { return discordUsername; }
}

