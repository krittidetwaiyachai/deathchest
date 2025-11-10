package xyz.kaijiieow.deathchest.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;

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
    private boolean isDisabling = false;

    public enum LogLevel {
        INFO(Level.INFO, 0x57F287),
        WARN(Level.WARNING, 0xFEE75C),
        ERROR(Level.SEVERE, 0xED4245);

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

    public void setDisabling() {
        this.isDisabling = true;
        log(LogLevel.INFO, "Logger กำลังสลับไปโหมด Synchronous เพื่อปิดเซิร์ฟเวอร์...", false);
    }

    public void log(LogLevel level, String message, boolean sendSimpleWebhook) {
        plugin.getLogger().log(level.getJavaLevel(), message);

        if (configManager.isFileLoggingEnabled() && fileLogger != null) {
            if (isDisabling) {
                fileLogger.log(level.getJavaLevel(), message);
            } else {
                final Level javaLevel = level.getJavaLevel();
                final String logMessage = message;
                
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    fileLogger.log(javaLevel, logMessage);
                });
            }
        }

        if (configManager.isDiscordLoggingEnabled() && sendSimpleWebhook) {
            if (isDisabling) return;
            sendSimpleDiscordWebhook(level, message);
        }
    }

    public void log(LogLevel level, String message) {
        log(level, message, true);
    }

    public void logDeath(Player player, String locationStr, int totalExp) {
        String msg = "สร้างกล่องศพให้ " + player.getName() + " ที่ " + locationStr + " (XP: " + totalExp + ")";
        log(LogLevel.INFO, msg, false);

        if (configManager.isDiscordLoggingEnabled() && !isDisabling) {
            sendRichDiscordWebhook(
                LogLevel.INFO,
                "💀 สร้างกล่องศพ",
                player.getName(),
                locationStr,
                totalExp,
                "ผู้เล่นตาย ระบบได้สร้างกล่องเก็บของและบันทึกพิกัดไว้ให้แล้ว"
            );
        }
    }

    public void logBuyback(Player player, int setIndex, int cost, String currency, int experience) {
        String msg = String.format(
            "%s ซื้อของคืน (ชุด %d) ราคา %d %s (ได้รับ XP: %d)",
            player.getName(), setIndex, cost, currency, experience
        );
        log(LogLevel.INFO, msg, false);

        if (configManager.isDiscordLoggingEnabled() && !isDisabling) {
            sendRichDiscordWebhook(
                LogLevel.INFO,
                "🛒 ซื้อของคืน",
                player.getName(),
                String.format("ชุด %d • ราคา %,d %s", setIndex, cost, currency),
                experience,
                "รายการซื้อของคืนสำเร็จ รายการของจะถูกส่งคืนตามสถานะล่าสุด"
            );
        }
    }

    public void logChestExpired(String playerName, String locationStr, int experience) {
        String msg = String.format(
            "กล่องศพของ %s ที่ %s หมดเวลา (XP: %d) - ย้ายไป /buyback",
            playerName, locationStr, experience
        );
        log(LogLevel.WARN, msg, false);

        if (configManager.isDiscordLoggingEnabled() && !isDisabling) {
            sendRichDiscordWebhook(
                LogLevel.WARN,
                "⌛ กล่องหมดเวลา",
                playerName,
                locationStr,
                experience,
                "กล่องศพหมดเวลา ของถูกย้ายไปที่ /buyback เรียบร้อย"
            );
        }
    }

    public void logChestCollected(String playerName, String locationStr) {
        String msg = String.format(
            "%s เก็บของจากกล่องศพที่ %s จนหมด กล่องถูกลบ",
            playerName, locationStr
        );
        log(LogLevel.INFO, msg, false);

        if (configManager.isDiscordLoggingEnabled() && !isDisabling) {
            sendRichDiscordWebhook(
                LogLevel.INFO,
                "✅ กล่องถูกเก็บ",
                playerName,
                locationStr,
                0,
                "ผู้เล่นเก็บของจากกล่องศพจนหมด กล่องถูกลบออกจากพื้นที่"
            );
        }
    }

    public void logAdminGuiOpen(Player admin, OfflinePlayer targetPlayer) {
        String msg = String.format(
            "แอดมิน %s เปิดดูรายการกล่องศพของ %s",
            admin.getName(), targetPlayer.getName()
        );
        log(LogLevel.WARN, msg, false);

        if (configManager.isDiscordLoggingEnabled() && !isDisabling) {
            sendRichDiscordWebhook(
                LogLevel.WARN,
                "👮‍ Admin ตรวจสอบ",
                admin.getName(),
                "เป้าหมาย: " + targetPlayer.getName(),
                null,
                "แอดมินเปิด GUI ดูรายการกล่องศพทั้งหมด (Active และ Buyback) ของผู้เล่น"
            );
        }
    }

    public void logAdminTpSuccess(Player admin, OfflinePlayer targetPlayer, String locationString) {
        String msg = String.format(
            "แอดมิน %s วาร์ปไปที่กล่องศพของ %s (ที่ %s)",
            admin.getName(), targetPlayer.getName(), locationString
        );
        log(LogLevel.WARN, msg, false);

        if (configManager.isDiscordLoggingEnabled() && !isDisabling) {
            sendRichDiscordWebhook(
                LogLevel.WARN,
                "🚀 Admin วาร์ป",
                admin.getName(),
                "เป้าหมาย: " + targetPlayer.getName(),
                null,
                "วาร์ปไปยังกล่องศพที่ Active ที่พิกัด: " + locationString
            );
        }
    }

    public void logAdminTpFailBuyback(Player admin, OfflinePlayer targetPlayer, int buybackIndex) {
        String msg = String.format(
            "แอดมิน %s พยายามวาร์ปไปที่กล่อง Buyback (Set %d) ของ %s แต่ไม่สำเร็จ",
            admin.getName(), buybackIndex + 1, targetPlayer.getName()
        );
        log(LogLevel.INFO, msg, false);
    }

    private String levelEmoji(LogLevel level) {
        switch (level) {
            case INFO: return "✅";
            case WARN: return "⚠️";
            case ERROR: return "❌";
            default: return "ℹ️";
        }
    }

    private String levelThai(LogLevel level) {
        switch (level) {
            case INFO: return "ข้อมูล";
            case WARN: return "คำเตือน";
            case ERROR: return "ข้อผิดพลาด";
            default: return "ข้อมูล";
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void sendSimpleDiscordWebhook(LogLevel level, String message) {
        if (!configManager.isDiscordLoggingEnabled()) return;
        
        String webhookUrl = configManager.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) return;

        String jsonPayload = String.format(
            "{\"username\":\"%s\",\"content\":\"%s %s\"}",
            escape(configManager.getDiscordUsername()),
            levelEmoji(level),
            escape(message)
        );

        postAsync(webhookUrl, jsonPayload);
    }

    private void sendRichDiscordWebhook(LogLevel level, String title, String playerName, String locationOrSet, Integer xp, String note) {
        if (!configManager.isDiscordLoggingEnabled()) return;
        
        String webhookUrl = configManager.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) return;

        StringBuilder embed = new StringBuilder();
        embed.append("{");
        embed.append("\"username\":\"").append(escape(configManager.getDiscordUsername())).append("\",");
        embed.append("\"embeds\":[{");
        embed.append("\"title\":\"").append(escape(title)).append("\",");
        embed.append("\"color\":").append(level.getDiscordColor()).append(",");
        embed.append("\"fields\":[");
        embed.append("{\"name\":\"ผู้เล่น\",\"value\":\"").append(escape(playerName)).append("\",\"inline\":true},");
        if (locationOrSet != null) {
            embed.append("{\"name\":\"ตำแหน่ง/ชุด\",\"value\":\"").append(escape(locationOrSet)).append("\",\"inline\":true},");
        }
        if (xp != null && xp > 0) {
            embed.append("{\"name\":\"XP\",\"value\":\"").append(xp).append("\",\"inline\":true},");
        }
        embed.append("{\"name\":\"รายละเอียด\",\"value\":\"").append(escape(note)).append("\",\"inline\":false}");
        embed.append("],");
        embed.append("\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\"");
        embed.append("}]}");
        embed.append("}");

        postAsync(webhookUrl, embed.toString());
    }

    private void postAsync(String webhookUrl, String jsonPayload) {
        if (isDisabling) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(webhookUrl).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("User-Agent", "Minecraft-DeathChest-Webhook");
                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                con.getResponseCode();
            } catch (Exception ignored) {
            } finally {
                if (con != null) con.disconnect();
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

