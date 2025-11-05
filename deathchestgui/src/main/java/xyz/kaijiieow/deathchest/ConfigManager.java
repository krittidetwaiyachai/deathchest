package xyz.kaijiieow.deathchest;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final DeathChestPlugin plugin;
    private int despawnTime;
    private String currencyName;
    private int buybackCost;
    private List<String> hologramLines;

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
        this.hologramLines = config.getStringList("settings.hologram-lines");

        this.fileLoggingEnabled = config.getBoolean("logging.file.enable", true);
        this.logFileName = config.getString("logging.file.log-name", "DeathChestGUI.log");
        this.discordLoggingEnabled = config.getBoolean("logging.discord.enable", false);
        this.discordWebhookUrl = config.getString("logging.discord.webhook-url", "YOUR_WEBHOOK_URL_HERE");
        this.discordUsername = config.getString("logging.discord.username", "DeathChest Logger");
    }

    public int getDespawnTime() { return despawnTime; }
    public String getCurrencyName() { return currencyName; }
    public int getBuybackCost() { return buybackCost; }
    public List<String> getHologramLines() { return hologramLines; }

    public boolean isFileLoggingEnabled() { return fileLoggingEnabled; }
    public String getLogFileName() { return logFileName; }
    public boolean isDiscordLoggingEnabled() { return discordLoggingEnabled; }
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public String getDiscordUsername() { return discordUsername; }
}