package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggingService {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private Logger fileLogger;

    public enum LogLevel {
        INFO(Level.INFO, 65280),
        WARN(Level.WARNING, 16776960),
        ERROR(Level.SEVERE, 16711680);

        private final Level javaLevel;
        private final int discordColor;

        LogLevel(Level javaLevel, int discordColor) {
            this.javaLevel = javaLevel;
            this.discordColor = discordColor;
        }

        public Level getJavaLevel() { return javaLevel; }
        public int getDiscordColor() { return discordColor; }
    }

    public LoggingService(DeathChestPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        if (configManager.isFileLoggingEnabled()) {
            setupFileLogger();
        }
    }

    private void setupFileLogger() {
        try {
            plugin.getDataFolder().mkdirs();
            fileLogger = Logger.getLogger("DeathChestFileLogger");
            FileHandler fh = new FileHandler(plugin.getDataFolder() + "/" + configManager.getLogFileName(), true);
            fh.setFormatter(new SimpleFormatter());
            fileLogger.addHandler(fh);
            fileLogger.setLevel(Level.INFO);
            fileLogger.setUseParentHandlers(false);
        } catch (Exception e) {
            plugin.getLogger().severe("ไม่สามารถสร้าง File logger ได้: " + e.getMessage());
        }
    }

    public void log(LogLevel level, String message) {
        plugin.getLogger().log(level.getJavaLevel(), message);

        if (configManager.isFileLoggingEnabled() && fileLogger != null) {
            fileLogger.log(level.getJavaLevel(), message);
        }

        if (configManager.isDiscordLoggingEnabled()) {
            sendSimpleDiscordWebhook(level, message);
        }
    }

    public void logDeath(Player player, String locationStr, int totalExp) {
        String msg = "สร้างกล่องศพให้ " + player.getName() + " ที่ " + locationStr + " (XP: " + totalExp + ")";
        
        log(LogLevel.INFO, msg); 
        
        if (configManager.isDiscordLoggingEnabled()) {
            sendRichDiscordWebhook(
                LogLevel.INFO, 
                "สร้างกล่องศพ", 
                player.getName(), 
                locationStr, 
                totalExp
            );
        }
    }

    public void logBuyback(Player player, int setIndex, int cost, String currency, int experience) {
        String msg = String.format("%s ซื้อของคืน (Set %d) ราคา %d %s (ได้ XP: %d)",
            player.getName(), setIndex, cost, currency, experience);
        
        log(LogLevel.INFO, msg);

        if (configManager.isDiscordLoggingEnabled()) {
            sendRichDiscordWebhook(
                LogLevel.INFO,
                "ซื้อของคืน",
                player.getName(),
                String.format("Set %d (ราคา %d %s)", setIndex, cost, currency),
                experience
            );
        }
    }

    private void sendSimpleDiscordWebhook(LogLevel level, String message) {
        String webhookUrl = configManager.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(webhookUrl).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                con.setDoOutput(true);

                String jsonPayload = String.format(
                    "{\"username\": \"%s\", \"embeds\": [{\"title\": \"[%s]\", \"description\": \"%s\", \"color\": %d}]}",
                    configManager.getDiscordUsername(),
                    level.name(),
                    message.replace("\"", "\\\""),
                    level.getDiscordColor()
                );

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                con.getResponseCode();
                con.disconnect();
            } catch (Exception e) { /* ไม่ต้อง spam console */ }
        });
    }

    private void sendRichDiscordWebhook(LogLevel level, String title, String playerName, String location, Integer xp) {
         String webhookUrl = configManager.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(webhookUrl).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                con.setDoOutput(true);

                String jsonPayload = String.format(
                    "{\"username\": \"%s\", \"embeds\": [{\"title\": \"%s\", \"color\": %d, \"fields\": [" +
                    "{\"name\": \"Player\", \"value\": \"%s\", \"inline\": true}," +
                    "{\"name\": \"Location/Set\", \"value\": \"%s\", \"inline\": true}," +
                    "{\"name\": \"Experience\", \"value\": \"%d\", \"inline\": true}" +
                    "]}]}",
                    configManager.getDiscordUsername(),
                    title,
                    level.getDiscordColor(),
                    playerName,
                    location,
                    xp
                );

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                con.getResponseCode();
                con.disconnect();
            } catch (Exception e) { /* ไม่ต้อง spam console */ }
        });
    }

    public void close() {
        if (fileLogger != null) {
            for (java.util.logging.Handler handler : fileLogger.getHandlers()) {
                handler.close();
            }
        }
    }
}