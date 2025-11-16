package xyz.kaijiieow.deathchest.manager;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
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
    private final StringBuilder hologramStringBuilder = new StringBuilder();
    
    private final Particle particleSoulFireFlame;
    private final Particle particleElectricSpark;
    private final Particle particleSculkSoul;
    
    public ChestTimer(DeathChestPlugin plugin, ConfigManager configManager, LoggingService logger,
                     Map<BlockLocation, DeathChestData> activeChests, ChestRemover chestRemover,
                     Particle particleSoulFireFlame, Particle particleElectricSpark, Particle particleSculkSoul) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
        this.activeChests = activeChests;
        this.chestRemover = chestRemover;
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

                    // --- 1. ตรวจสอบและนับเวลา (ทำงานตลอด) ---
                    if (data.timeLeft <= 0) {
                        chestsToRemove.add(key);
                        continue; // กล่องนี้หมดเวลา, ไปอันต่อไป
                    }
                    
                    data.timeLeft--; // นับเวลาถอยหลัง 1 วินาที (ทำงานตลอด)

                    // --- 2. ตรวจสอบ Chunk ก่อนทำสิ่งที่ต้องโหลด ---
                    World world = Bukkit.getWorld(key.worldName());
                    if (world == null) {
                        // โลกไม่โหลด ข้ามไป (เวลานับไปแล้ว)
                        continue;
                    }
                    
                    Location chestLoc = new Location(world, key.x(), key.y(), key.z());
                    if (!world.isChunkLoaded(chestLoc.getChunk())) {
                        // Chunk ไม่โหลด ข้ามไป (เวลานับไปแล้ว)
                        // ไม่ต้องพยายามทำอะไรกับ Hologram/Particle
                        continue; 
                    }

                    // --- 3. (ถ้ามาถึงนี่ = Chunk โหลดอยู่) ตรวจสอบ/สร้าง Hologram ---
                    if (data.hologramEntity == null || !data.hologramEntity.isValid()) {
                        // (Chunk โหลดอยู่ แต่ Hologram หาย (อาจจะโดน /kill) -> สร้างใหม่)
                        logger.log(LoggingService.LogLevel.WARN, "โฮโลแกรมของ " + data.ownerName + " หาย! (Chunk โหลดอยู่) พยายามสร้างใหม่...");
                        try {
                            Location hologramLoc = new Location(world, key.x() + 0.5, key.y() + configManager.getHologramYOffset(), key.z() + 0.5);

                            int minutes = data.timeLeft / 60;
                            int seconds = data.timeLeft % 60;
                            final String timeString;
                            if (minutes > 0) {
                                timeString = minutes + " นาที " + seconds;
                            } else {
                                timeString = String.valueOf(data.timeLeft);
                            }

                            hologramStringBuilder.setLength(0);
                            List<String> lines = configManager.getHologramLines();
                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);
                                line = line.replace("&", "§")
                                        .replace("%player%", data.ownerName)
                                        .replace("%time%", timeString)
                                        .replace("%xp%", String.valueOf(data.experience))
                                        .replace("%coords%", data.locationString);

                                hologramStringBuilder.append(line);
                                if (i < lines.size() - 1) {
                                    hologramStringBuilder.append("\n");
                                }
                            }
                            final String newText = hologramStringBuilder.toString();

                            TextDisplay newHologram = world.spawn(hologramLoc, TextDisplay.class, (holo) -> {
                                holo.setGravity(false);
                                holo.setPersistent(true); // <--- *** นี่คือจุดที่แก้ ***
                                holo.setInvulnerable(true);
                                holo.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                                holo.setAlignment(TextDisplay.TextAlignment.CENTER);
                                holo.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                                holo.setGlowing(true);
                                holo.setGlowColorOverride(Color.YELLOW);
                                holo.setViewRange(64.0f);
                                holo.setText(newText);
                            });

                            data.hologramEntity = newHologram;
                            
                        } catch (Exception e) {
                            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถสร้างโฮโลแกรมใหม่ให้กับ " + data.ownerName + " ได้: " + e.getMessage());
                        }
                    }

                    // --- 4. (ถ้า Chunk โหลดอยู่) สร้าง Particle ---
                    if (showParticles) {
                        try {
                            Location center = new Location(world, key.x() + 0.5, key.y() + 0.5, key.z() + 0.5);
                            world.spawnParticle(particleSoulFireFlame, center, 5, 0.5, 0.5, 0.5, 0.02);
                            world.spawnParticle(Particle.TOTEM, center.clone().add(0, 0.5, 0), 1, 0.3, 0.5, 0.3, 0.1);
                            world.spawnParticle(particleElectricSpark, center, 2, 0.5, 0.5, 0.5, 0.05);
                            world.spawnParticle(particleSculkSoul, center, 1, 0.5, 0.5, 0.5, 0.02);
                        } catch (Exception e) {
                             // ไม่ต้อง spam log ถ้า particle เฟล
                        }
                    }

                    // --- 5. (ถ้า Chunk โหลดอยู่) อัปเดตข้อความ Hologram ---
                    int minutes = data.timeLeft / 60;
                    int seconds = data.timeLeft % 60;

                    final String timeString;
                    if (minutes > 0) {
                        timeString = minutes + " นาที " + seconds;
                    } else {
                        timeString = String.valueOf(data.timeLeft);
                    }

                    hologramStringBuilder.setLength(0);
                    List<String> lines = configManager.getHologramLines();
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        line = line.replace("&", "§")
                                .replace("%player%", data.ownerName)
                                .replace("%time%", timeString)
                                .replace("%xp%", String.valueOf(data.experience))
                                .replace("%coords%", data.locationString);

                        hologramStringBuilder.append(line);
                        if (i < lines.size() - 1) {
                            hologramStringBuilder.append("\n");
                        }
                    }

                    if (data.hologramEntity != null && data.hologramEntity.isValid()) {
                        data.hologramEntity.setText(hologramStringBuilder.toString());
                    }
                }

                // --- 6. ลบกล่องที่หมดเวลา (นอกลูปหลัก) ---
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