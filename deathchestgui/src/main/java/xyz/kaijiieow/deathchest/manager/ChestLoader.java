package xyz.kaijiieow.deathchest.manager;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.kaijiieow.deathchest.database.ChestDatabase;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.model.DatabaseChestData;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.util.SerializationUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChestLoader {
    
    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final LoggingService logger;
    private final ChestDatabase chestDatabase;
    private final Map<BlockLocation, DeathChestData> activeChests;
    private final Map<UUID, List<BlockLocation>> playerChestMap;
    private final StringBuilder hologramStringBuilder = new StringBuilder();
    
    public ChestLoader(DeathChestPlugin plugin, ConfigManager configManager, LoggingService logger,
                      ChestDatabase chestDatabase,
                      Map<BlockLocation, DeathChestData> activeChests,
                      Map<UUID, List<BlockLocation>> playerChestMap) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
        this.chestDatabase = chestDatabase;
        this.activeChests = activeChests;
        this.playerChestMap = playerChestMap;
    }
    
    public void staggerLoadChests(List<DatabaseChestData> dbChests) {
        if (dbChests == null || dbChests.isEmpty()) {
            logger.log(LoggingService.LogLevel.INFO, "ไม่มีกล่องศพที่ต้องโหลด");
            return;
        }
        
        logger.log(LoggingService.LogLevel.INFO, "กำลังโหลดกล่องศพ " + dbChests.size() + " กล่อง (แบบ Staggered)...");
        
        final int[] index = {0};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (index[0] >= dbChests.size()) {
                    logger.log(LoggingService.LogLevel.INFO, "โหลดกล่องศพเสร็จสิ้น " + dbChests.size() + " กล่อง");
                    cancel();
                    return;
                }
                
                DatabaseChestData dbData = dbChests.get(index[0]);
                loadChest(dbData);
                index[0]++;
            }
        }.runTaskTimer(plugin, 1L, 2L); // Load 1 chest every 2 ticks (0.1 seconds)
    }
    
    private void loadChest(DatabaseChestData dbData) {
        try {
            World world = Bukkit.getWorld(dbData.world);
            if (world == null) {
                logger.log(LoggingService.LogLevel.WARN, "ไม่สามารถโหลดกล่องศพของ " + dbData.ownerName + " ได้เพราะโลก '" + dbData.world + "' ไม่ได้โหลดอยู่");
                return;
            }
            
            Location blockLoc = new Location(world, dbData.x, dbData.y, dbData.z);
            
            // Check if block is already a chest or if location is occupied
            if (blockLoc.getBlock().getType() != Material.AIR && blockLoc.getBlock().getType() != Material.CHEST) {
                logger.log(LoggingService.LogLevel.WARN, "ไม่สามารถโหลดกล่องศพของ " + dbData.ownerName + " ได้เพราะตำแหน่งถูกครอบครองแล้ว");
                return;
            }
            
            // Place chest
            blockLoc.getBlock().setType(Material.CHEST);
            Chest chest = (Chest) blockLoc.getBlock().getState();
            
            // Deserialize items
            ItemStack[] items;
            try {
                items = SerializationUtils.itemStackArrayFromBase64(dbData.itemsBase64);
            } catch (Exception e) {
                logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถ deserialize items ของ " + dbData.ownerName + " ได้: " + e.getMessage());
                blockLoc.getBlock().setType(Material.AIR);
                return;
            }
            
            // Put items in chest
            chest.getInventory().setContents(items);
            chest.update();
            
            // Create hologram
            Location hologramLoc = blockLoc.clone().add(0.5, configManager.getHologramYOffset(), 0.5);
            String locationStr = String.format("%d, %d, %d", dbData.x, dbData.y, dbData.z);
            int minutes = dbData.remainingSeconds / 60;
            int seconds = dbData.remainingSeconds % 60;
            final String timeString;
            if (minutes > 0) {
                timeString = minutes + " นาที " + seconds;
            } else {
                timeString = String.valueOf(dbData.remainingSeconds);
            }
            
            hologramStringBuilder.setLength(0);
            List<String> lines = configManager.getHologramLines();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                line = line.replace("&", "§")
                        .replace("%player%", dbData.ownerName)
                        .replace("%time%", timeString)
                        .replace("%xp%", String.valueOf(dbData.experience))
                        .replace("%coords%", locationStr);
                
                hologramStringBuilder.append(line);
                if (i < lines.size() - 1) {
                    hologramStringBuilder.append("\n");
                }
            }
            final String hologramText = hologramStringBuilder.toString();
            
            TextDisplay hologram = world.spawn(hologramLoc, TextDisplay.class, (holo) -> {
                holo.setGravity(false);
                holo.setPersistent(false);
                holo.setInvulnerable(true);
                holo.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                holo.setAlignment(TextDisplay.TextAlignment.CENTER);
                holo.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                holo.setGlowing(true);
                holo.setGlowColorOverride(Color.YELLOW);
                holo.setViewRange(64.0f);
                holo.setText(hologramText);
            });
            
            // Create DeathChestData
            // ใช้ remainingSeconds เป็น initialDespawnTime สำหรับกล่องที่โหลดจาก database
            // (อาจจะไม่ถูกต้อง 100% แต่ก็พอใช้ได้สำหรับการคำนวณ protection)
            int estimatedInitialTime = dbData.remainingSeconds;
            UUID ownerUUID = UUID.fromString(dbData.ownerUuid);
            DeathChestData data = new DeathChestData(
                ownerUUID, dbData.ownerName, chest, hologram,
                items, dbData.experience, locationStr,
                dbData.world, dbData.x, dbData.y, dbData.z,
                dbData.createdAt, estimatedInitialTime
            );
            data.timeLeft = dbData.remainingSeconds;
            
            // Add to maps
            BlockLocation key = new BlockLocation(dbData.world, dbData.x, dbData.y, dbData.z);
            activeChests.put(key, data);
            playerChestMap.putIfAbsent(ownerUUID, new java.util.ArrayList<>());
            playerChestMap.get(ownerUUID).add(key);
            
        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "เกิดข้อผิดพลาดตอนโหลดกล่องศพของ " + dbData.ownerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

