package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggingService {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private Logger fileLogger;

    public enum LogLevel {
        INFO(Level.INFO, 0x57F287),   // เขียวมิ้นต์ Discord
        WARN(Level.WARNING, 0xFEE75C),// เหลือง
        ERROR(Level.SEVERE, 0xED4245);// แดง

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

    // ===================== Public APIs =====================

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
        log(LogLevel.INFO, msg);

        if (configManager.isDiscordLoggingEnabled()) {
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

    // [NEW] Added this method
    public void logChestExpired(String playerName, String locationStr, int experience) {
        String msg = String.format(
            "กล่องศพของ %s ที่ %s หมดเวลา (XP: %d) - ย้ายไป /buyback",
            playerName, locationStr, experience
        );
        log(LogLevel.WARN, msg); // ใช้ WARN เพราะเป็นสีเหลือง

        if (configManager.isDiscordLoggingEnabled()) {
            sendRichDiscordWebhook(
                LogLevel.WARN, // สีเหลือง
                "⌛ กล่องหมดเวลา",
                playerName,
                locationStr,
                experience,
                "กล่องศพหมดเวลา ของถูกย้ายไปที่ /buyback เรียบร้อย"
            );
        }
    }

    // [NEW] Added this method
    public void logChestCollected(String playerName, String locationStr) {
        String msg = String.format(
            "%s เก็บของจากกล่องศพที่ %s จนหมด กล่องถูกลบ",
            playerName, locationStr
        );
        log(LogLevel.INFO, msg);

        if (configManager.isDiscordLoggingEnabled()) {
            sendRichDiscordWebhook(
                LogLevel.INFO, // สีเขียว
                "✅ กล่องถูกเก็บ",
                playerName,
                locationStr,
                0, // XP ถูกเก็บไปก่อนหน้านี้แล้ว
                "ผู้เล่นเก็บของจากกล่องศพจนหมด กล่องถูกลบออกจากพื้นที่"
            );
        }
    }

    // ===================== Discord Helpers =====================

    private String levelEmoji(LogLevel level) {
        switch (level) {
            case INFO:  return "ℹ️";
            case WARN:  return "⚠️";
            case ERROR: return "⛔";
            default:    return "🔔";
        }
    }

    private String levelThai(LogLevel level) {
        switch (level) {
            case INFO:  return "ข้อมูล";
            case WARN:  return "คำเตือน";
            case ERROR: return "ข้อผิดพลาด";
            default:    return "แจ้งเตือน";
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        // Escape สำหรับ JSON แบบง่ายพอใช้กับ Discord
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private void sendSimpleDiscordWebhook(LogLevel level, String message) {
        String webhookUrl = configManager.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        String title = levelEmoji(level) + " " + levelThai(level);
        // จัดรูปแบบให้อ่านง่ายเป็นภาษาไทย + เว้นบรรทัด
        String description =
              "```" + levelThai(level) + "```"
            + "รายละเอียด:\n"
            + escape(message) + "\n\n"
            + "🕒 เวลา: <t:" + Instant.now().getEpochSecond() + ":F>";

        String jsonPayload =
            "{"
                + "\"username\":\"" + escape(configManager.getDiscordUsername()) + "\","
                + "\"allowed_mentions\":{\"parse\":[]},"
                + "\"embeds\":[{"
                    + "\"title\":\"" + escape(title) + "\","
                    + "\"description\":\"" + description + "\","
                    + "\"color\":" + level.getDiscordColor() + ","
                    + "footer\":{\"text\":\"" + escape(plugin.getName()) + " • MyServer SMP\"},"
                    + "\"timestamp\":\"" + Instant.now().toString() + "\""
                + "}]"
            + "}";

        postAsync(webhookUrl, jsonPayload);
    }

    private void sendRichDiscordWebhook(LogLevel level, String title, String playerName, String locationOrSet, Integer xp, String note) {
        String webhookUrl = configManager.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        String header = levelEmoji(level) + " " + title;
        String desc =
              (note != null && !note.isBlank() ? escape(note) + "\\n\\n" : "")
            + "🕒 เวลา: <t:" + Instant.now().getEpochSecond() + ":F>";

        String fields =
              "{"
                + "\"name\":\"ผู้เล่น\","
                + "\"value\":\"" + escape(playerName) + "\","
                + "\"inline\":true"
              + "},"
              + "{"
                + "\"name\":\"ตำแหน่ง/ชุด\","
                + "\"value\":\"" + escape(locationOrSet) + "\","
                + "\"inline\":true"
              + "},"
              + "{"
                + "\"name\":\"ค่าประสบการณ์\","
                + "\"value\":\"" + (xp == null ? "-" : xp.toString()) + "\","
                + "\"inline\":true"
              + "}";

        String jsonPayload =
            "{"
                + "\"username\":\"" + escape(configManager.getDiscordUsername()) + "\","
                + "\"allowed_mentions\":{\"parse\":[]},"
                + "\"embeds\":[{"
                    + "\"title\":\"" + escape(header) + "\","
                    + "\"description\":\"" + desc + "\","
                    + "\"color\":" + level.getDiscordColor() + ","
                    + "\"fields\":[" + fields + "],"
                    + "\"footer\":{\"text\":\"" + escape(plugin.getName()) + " • " + escape(Bukkit.getServer().getName()) + "\"},"
                    + "\"timestamp\":\"" + Instant.now().toString() + "\""
                + "}]"
            + "}";

        postAsync(webhookUrl, jsonPayload);
    }

    private void postAsync(String webhookUrl, String jsonPayload) {
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
                // fire the request
                con.getResponseCode();
            } catch (Exception ignored) {
                // ไม่ต้อง spam console
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