package xyz.kaijiieow.deathchest;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final DeathChestPlugin plugin;
    private int despawnTime;
    private String currencyName;
    private int buybackCost;
    private boolean allowOtherPlayersToOpen;
    private double hologramYOffset;
    private List<String> hologramLines;

    private String chatMessageDeath;
    private String chatMessageExpired;
    private String chatMessageNotYourChest;
    private String chatMessageXpRestored;

    private boolean fileLoggingEnabled;
    private String logFileName;
    private boolean discordLoggingEnabled;
    private String discordWebhookUrl;
    private String discordUsername;

    public ConfigManager(DeathChestPlugin plugin) {
        this.plugin = plugin;
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();
        loadConfigValues();
    }

    private void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();
        this.despawnTime = config.getInt("settings.despawn-timer", 300);
        this.currencyName = config.getString("settings.currency-name", "coins");
        this.buybackCost = config.getInt("settings.buyback-cost", 1000);
        this.allowOtherPlayersToOpen = config.getBoolean("settings.allow-other-players-to-open", false);
        this.hologramYOffset = config.getDouble("settings.hologram-y-offset", 1.2);
        this.hologramLines = config.getStringList("settings.hologram-lines");

        this.chatMessageDeath = config.getString("messages.death", "§cคุณตาย! ของของคุณอยู่ในกล่องที่ตำแหน่ง: §f%coords% §c(XP: §f%xp%§c)");
        this.chatMessageExpired = config.getString("messages.expired", "§cกล่องเก็บของของคุณหมดเวลา! §eคุณสามารถซื้อคืนได้ด้วยคำสั่ง §f/buyback");
        this.chatMessageNotYourChest = config.getString("messages.not_your_chest", "§cนี่ไม่ใช่กล่องศพของคุณ!");
        this.chatMessageXpRestored = config.getString("messages.xp_restored", "§aคุณได้รับ XP คืน: %xp%");

        this.fileLoggingEnabled = config.getBoolean("logging.file.enable", true);
        this.logFileName = config.getString("logging.file.log-name", "DeathChestGUI.log");
        this.discordLoggingEnabled = config.getBoolean("logging.discord.enable", false);
        this.discordWebhookUrl = config.getString("logging.discord.webhook-url", "YOUR_WEBHOOK_URL_HERE");
        this.discordUsername = config.getString("logging.discord.username", "DeathChest Logger");
    }

    public int getDespawnTime() { return despawnTime; }
    public String getCurrencyName() { return currencyName; }
    public int getBuybackCost() { return buybackCost; }
    public boolean isAllowOtherPlayersToOpen() { return allowOtherPlayersToOpen; }
    public double getHologramYOffset() { return hologramYOffset; }
    public List<String> getHologramLines() { return hologramLines; }

    public String getChatMessageDeath() { return chatMessageDeath; }
    public String getChatMessageExpired() { return chatMessageExpired; }
    public String getChatMessageNotYourChest() { return chatMessageNotYourChest; }
    public String getChatMessageXpRestored() { return chatMessageXpRestored; }

    public boolean isFileLoggingEnabled() { return fileLoggingEnabled; }
    public String getLogFileName() { return logFileName; }
    public boolean isDiscordLoggingEnabled() { return discordLoggingEnabled; }
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public String getDiscordUsername() { return discordUsername; }
}