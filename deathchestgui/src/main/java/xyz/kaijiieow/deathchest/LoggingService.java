package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
    private boolean isDisabling = false; // [FIX] เพิ่ม Flag นี้

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

    // [FIX] เพิ่มเมธอดนี้ ให้ DeathChestPlugin เรียก
    public void setDisabling() {
        this.isDisabling = true;
        log(LogLevel.INFO, "Logger กำลังสลับไปโหมด Synchronous เพื่อปิดเซิร์ฟเวอร์...", false);
    }


    // ===================== Public APIs =====================

    public void log(LogLevel level, String message, boolean sendSimpleWebhook) {
        // 1. Log to Console (Sync - อันนี้จำเป็นต้อง Sync)
        plugin.getLogger().log(level.getJavaLevel(), message);

        // [FIX] เช็ก Flag ก่อนรัน Async
        if (configManager.isFileLoggingEnabled() && fileLogger != null) {
            if (isDisabling) {
                // ถ้ากำลังปิด -> Log แบบธรรมดา (Sync)
                fileLogger.log(level.getJavaLevel(), message);
            } else {
                // ถ้าทำงานปกติ -> Log แบบ Async
                final Level javaLevel = level.getJavaLevel();
                final String logMessage = message; 
                
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    fileLogger.log(javaLevel, logMessage);
                });
            }
        }

        // [FIX] เช็ก Flag ก่อนส่ง Discord
        if (configManager.isDiscordLoggingEnabled() && sendSimpleWebhook) {
            if (isDisabling) return; // กำลังปิด, ไม่ต้องส่ง
            sendSimpleDiscordWebhook(level, message);
        }
    }

    public void log(LogLevel level, String message) {
        log(level, message, true); // Default to sending a simple webhook
    }

    public void logDeath(Player player, String locationStr, int totalExp) {
        String msg = "สร้างกล่องศพให้ " + player.getName() + " ที่ " + locationStr + " (XP: " + totalExp + ")";
        log(LogLevel.INFO, msg, false); 

        // [FIX] เช็ก Flag
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

        // [FIX] เช็ก Flag
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

        // [FIX] เช็ก Flag
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

        // [FIX] เช็ก Flag
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

    // ===================== [NEW] Admin Logs =====================

    public void logAdminGuiOpen(Player admin, OfflinePlayer targetPlayer) {
        String msg = String.format(
            "แอดมิน %s เปิดดูรายการกล่องศพของ %s",
            admin.getName(), targetPlayer.getName()
        );
        log(LogLevel.WARN, msg, false); 

        // [FIX] เช็ก Flag
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

        // [FIX] เช็ก Flag
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


    // ===================== Discord Helpers =====================
    // ... (levelEmoji, levelThai, escape, sendSimpleDiscordWebhook... โค้ดพวกนี้เหมือนเดิม) ...
    // (แต่เราจะแก้ postAsync)

    private String levelEmoji(LogLevel level) { /* ... โค้ดเดิม ... */ return ""; }
    private String levelThai(LogLevel level) { /* ... โค้ดเดิม ... */ return ""; }
    private String escape(String s) { /* ... โค้ดเดิม ... */ return ""; }
    private void sendSimpleDiscordWebhook(LogLevel level, String message) { /* ... โค้ดเดิม ... */ }
    private void sendRichDiscordWebhook(LogLevel level, String title, String playerName, String locationOrSet, Integer xp, String note) { /* ... โค้ดเดิม ... */ }


    private void postAsync(String webhookUrl, String jsonPayload) {
        // [FIX] เช็ก Flag ก่อนยิง
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
                // ไม่ต้อง spam console
            } finally {
                if (con != null) con.disconnect();
            }
        });
    }

    public void close() {
        // [FIX] setDisabling() ถูกย้ายไปเรียกก่อนใน onDisable
        // ที่นี่เหลือแค่ปิด handler
        if (fileLogger != null) {
            for (java.util.logging.Handler handler : fileLogger.getHandlers()) {
                handler.close();
            }
        }
    }
}