package xyz.kaijiieow.deathchest.manager;

import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.kaijiieow.deathchest.hologram.FancyHologramService;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.util.LoggingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChestTimer {
    
    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final LoggingService logger;
    private final Map<BlockLocation, DeathChestData> activeChests;
    private final ChestRemover chestRemover;
    private final FancyHologramService hologramService;
    
    private final Particle particleSoulFireFlame;
    private final Particle particleElectricSpark;
    private final Particle particleSculkSoul;
    
    public ChestTimer(DeathChestPlugin plugin, ConfigManager configManager, LoggingService logger,
                     Map<BlockLocation, DeathChestData> activeChests, ChestRemover chestRemover,
                     FancyHologramService hologramService,
                     Particle particleSoulFireFlame, Particle particleElectricSpark, Particle particleSculkSoul) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
        this.activeChests = activeChests;
        this.chestRemover = chestRemover;
        this.hologramService = hologramService;
        this.particleSoulFireFlame = particleSoulFireFlame;
        this.particleElectricSpark = particleElectricSpark;
        this.particleSculkSoul = particleSculkSoul;
    }
    
    public void startGlobalTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeChests.isEmpty()) {
                    return;
                }

                List<BlockLocation> chestsToRemove = new ArrayList<>();
                boolean showParticles = configManager.isShowParticles();

                for (Map.Entry<BlockLocation, DeathChestData> entry : activeChests.entrySet()) {
                    BlockLocation key = entry.getKey();
                    DeathChestData data = entry.getValue();

                    if (!hologramService.isValid(data.hologramEntity)) {
                        logger.log(LoggingService.LogLevel.WARN, "โฮโลแกรมของ " + data.ownerName + " หาย ตรวจพบความเสียหายของกล่องศพ ลบกล่องนี้ทิ้ง");
                        chestsToRemove.add(key);
                        continue;
                    }

                    if (data.timeLeft <= 0) {
                        chestsToRemove.add(key);
                        continue;
                    }

                    if (showParticles) {
                        try {
                            World world = Bukkit.getWorld(key.worldName());
                            if (world != null) {
                                Location center = new Location(world, key.x() + 0.5, key.y() + 0.5, key.z() + 0.5);
                                world.spawnParticle(particleSoulFireFlame, center, 5, 0.5, 0.5, 0.5, 0.02);
                                world.spawnParticle(Particle.TOTEM, center.clone().add(0, 0.5, 0), 1, 0.3, 0.5, 0.3, 0.1);
                                world.spawnParticle(particleElectricSpark, center, 2, 0.5, 0.5, 0.5, 0.05);
                                world.spawnParticle(particleSculkSoul, center, 1, 0.5, 0.5, 0.5, 0.02);
                            }
                        } catch (Exception e) {
                            logger.log(LoggingService.LogLevel.WARN, "เกิดข้อผิดพลาดตอนสร้าง Particle: " + e.getMessage());
                        }
                    }

                    hologramService.updateDeathChestHologram(
                            data.hologramEntity,
                            data.ownerName,
                            data.experience,
                            data.locationString,
                            data.timeLeft
                    );

                    data.timeLeft--;
                }

                for (BlockLocation keyToRemove : chestsToRemove) {
                    DeathChestData dataToRemove = activeChests.get(keyToRemove);
                    if (dataToRemove != null) {
                        chestRemover.removeChest(keyToRemove, dataToRemove, true);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}

