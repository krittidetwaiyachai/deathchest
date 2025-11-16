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
        deleteById(hologramId);

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

    public void delete(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        try {
            hologramManager.removeHologram(hologram);
        } catch (Exception ignored) {
        }
        try {
            hologram.deleteHologram();
        } catch (Exception ignored) {
        }
    }

    public void deleteById(String hologramId) {
        if (hologramId == null) {
            return;
        }
        hologramManager.getHologram(hologramId).ifPresent(this::delete);
    }

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
