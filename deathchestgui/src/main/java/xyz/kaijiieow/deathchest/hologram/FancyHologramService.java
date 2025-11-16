package xyz.kaijiieow.deathchest.hologram;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.util.LoggingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Wrapper around the FancyHolograms API so the rest of the plugin
 * does not need to deal with the raw API classes directly.
 */
public class FancyHologramService {

    private final ConfigManager configManager;
    private final LoggingService logger;
    private final HologramManager hologramManager;

    public FancyHologramService(ConfigManager configManager, LoggingService logger) {
        this.configManager = configManager;
        this.logger = logger;

        if (!FancyHologramsPlugin.isEnabled()) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่พบปลั๊กอิน FancyHolograms ปลั๊กอินนี้จำเป็นต้องใช้งานร่วมกัน");
            throw new IllegalStateException("FancyHolograms plugin is required but not enabled");
        }

        this.hologramManager = FancyHologramsPlugin.get().getHologramManager();
    }

    public Hologram createDeathChestHologram(String hologramId, Location location, String ownerName,
                                             int experience, String coordinates, int secondsRemaining) {
        TextHologramData data = new TextHologramData(hologramId, location);
        configureDefaults(data);
        data.setText(buildFormattedLines(ownerName, experience, coordinates, secondsRemaining));

        // ป้องกันไม่ให้สร้างโฮโลแกรมซ้ำชื่อเดิมค้างอยู่
        // เราจะ 'ปิด' การ log error ตรงนี้ เพราะมันน่ารำคาญ
        // deleteById(hologramId);
        hologramManager.getHologram(hologramId).ifPresent(this::deleteQuietly); // <--- แก้ไขไม่ให้ log error ตอนสร้าง

        Hologram hologram = hologramManager.create(data);
        hologramManager.addHologram(hologram);
        return hologram;
    }

    public void updateDeathChestHologram(Hologram hologram, String ownerName, int experience,
                                         String coordinates, int secondsRemaining) {
        if (hologram == null) {
            return;
        }
        if (!(hologram.getData() instanceof TextHologramData textData)) {
            return;
        }

        textData.setText(buildFormattedLines(ownerName, experience, coordinates, secondsRemaining));
        hologram.forceUpdate();
    }

    // --- START CORRECTION (รอบ 3) ---

    // เมธอดลบแบบส่งเสียงดัง (สำหรับ ChestRemover)
    public void delete(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        
        // สลับลำดับการลบ
        // ลองเรียก deleteHologram() ก่อน
        try {
            hologram.deleteHologram(); // [Line 82]
        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "ลบ Hologram (delete) ไม่สำเร็จ: " + e.getMessage()); // [Line 85]
        }
        
        // แล้วค่อยเรียก removeHologram()
        try {
            hologramManager.removeHologram(hologram); // [Line 78]
        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "ลบ Hologram (remove) ไม่สำเร็จ: " + e.getMessage()); // [Line 80]
        }
    }

    // เมธอดลบแบบเงียบๆ (สำหรับตอนสร้าง)
    private void deleteQuietly(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        try {
            hologram.deleteHologram();
        } catch (Exception ignored) {
        }
        try {
            hologramManager.removeHologram(hologram);
        } catch (Exception ignored) {
        }
    }

    // เมธอด deleteById (สำหรับ ChestRemover)
    public void deleteById(String hologramId) {
        if (hologramId == null) {
            logger.log(LoggingService.LogLevel.WARN, "พยายามลบโฮโลแกรม แต่ hologramId เป็น null");
            return;
        }
        
        Optional<Hologram> holo = hologramManager.getHologram(hologramId);
        
        if (holo.isPresent()) {
            logger.log(LoggingService.LogLevel.INFO, "เจอโฮโลแกรมด้วย ID: " + hologramId + " - กำลังสั่งลบ...");
            delete(holo.get()); // เรียกใช้เมธอด delete() ที่ส่งเสียงดัง
        } else {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่เจอโฮโลแกรมด้วย ID: " + hologramId + " - การลบจึงล้มเหลว (ปลั๊กอิน FancyHolograms หาไม่เจอ)");
        }
    }
    
    // --- END CORRECTION (รอบ 3) ---

    public boolean isValid(Hologram hologram) {
        if (hologram == null) {
            return false;
        }
        return hologramManager.getHologram(hologram.getName()).isPresent();
    }

    public String buildHologramId(String worldName, int x, int y, int z, UUID ownerId) {
        String shortId = ownerId.toString().substring(0, 8);
        return String.format("deathchest_%s_%d_%d_%d_%s", worldName, x, y, z, shortId);
    }

    private void configureDefaults(TextHologramData data) {
        data.setPersistent(false);
        data.setVisibilityDistance(64);
        data.setSeeThrough(true);
        data.setTextShadow(true);
        data.setTextAlignment(TextDisplay.TextAlignment.CENTER);
        data.setBillboard(Display.Billboard.CENTER);
        data.setBrightness(new Display.Brightness(15, 15));
    }

    private List<String> buildFormattedLines(String ownerName, int experience, String coordinates, int seconds) {
        int safeSeconds = Math.max(seconds, 0);
        int minutes = safeSeconds / 60;
        int remainingSeconds = safeSeconds % 60;

        final String timeString = minutes > 0
                ? minutes + " นาที " + remainingSeconds
                : String.valueOf(remainingSeconds);

        List<String> template = configManager.getHologramLines();
        List<String> formatted = new ArrayList<>(template.size());
        for (String line : template) {
            formatted.add(line
                    .replace("&", "§")
                    .replace("%player%", ownerName)
                    .replace("%time%", timeString)
                    .replace("%xp%", String.valueOf(Math.max(experience, 0)))
                    .replace("%coords%", coordinates));
        }
        return formatted;
    }
}