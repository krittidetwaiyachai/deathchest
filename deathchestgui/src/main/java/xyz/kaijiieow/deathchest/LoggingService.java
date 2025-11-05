package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;

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
        INFO(Level.INFO, 65280), // Green
        WARN(Level.WARNING, 16776960), // Yellow
        ERROR(Level.SEVERE, 16711680); // Red

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
            fileLogger.setUseParentHandlers(false); // ไม่ต้องซ้ำกับ console
        } catch (Exception e) {
            plugin.getLogger().severe("ไม่สามารถสร้าง File logger ได้: " + e.getMessage());
        }
    }

    public void log(LogLevel level, String message) {
        // 1. Log to Console (เสมอ)
        plugin.getLogger().log(level.getJavaLevel(), message);

        // 2. Log to File
        if (configManager.isFileLoggingEnabled() && fileLogger != null) {
            fileLogger.log(level.getJavaLevel(), message);
        }

        // 3. Log to Discord (Async)
        if (configManager.isDiscordLoggingEnabled()) {
            sendDiscordWebhook(level, message);
        }
    }

    private void sendDiscordWebhook(LogLevel level, String message) {
        String webhookUrl = configManager.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                con.setDoOutput(true);

                // สร้าง JSON Payload
                String jsonPayload = String.format(
                    "{\"username\": \"%s\", \"embeds\": [{\"title\": \"[%s]\", \"description\": \"%s\", \"color\": %d}]}",
                    configManager.getDiscordUsername(),
                    level.name(),
                    message.replace("\"", "\\\""), // Escape quotes
                    level.getDiscordColor()
                );

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // ส่ง Request
                con.getResponseCode();
                con.disconnect();

            } catch (Exception e) {
                // ไม่ต้อง spam console ถ้ายิง webhook ไม่ผ่าน
                // plugin.getLogger().warning("ยิง Discord webhook ไม่สำเร็จ: " + e.getMessage());
            }
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