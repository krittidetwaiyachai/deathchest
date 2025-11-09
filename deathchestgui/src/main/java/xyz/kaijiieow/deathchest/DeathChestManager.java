package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.kaijiieow.deathchest.LoggingService.LogLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; 

public class DeathChestManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final LoggingService logger;
    private final DatabaseManager databaseManager;

    private final Map<BlockLocation, DeathChestData> activeChests = new ConcurrentHashMap<>();
    private final Map<UUID, List<BlockLocation>> playerChestMap = new ConcurrentHashMap<>(); 
    
    private final Particle particleSoulFireFlame = resolveParticle("SOUL_FIRE_FLAME", Particle.FLAME);
    private final Particle particleElectricSpark = resolveParticle("ELECTRIC_SPARK", Particle.FIREWORKS_SPARK);
    private final Particle particleSculkSoul = resolveParticle("SCULK_SOUL", Particle.PORTAL);

    private final StringBuilder hologramStringBuilder = new StringBuilder();

    public DeathChestManager(DeathChestPlugin plugin, ConfigManager configManager, StorageManager storageManager, LoggingService logger, DatabaseManager databaseManager) { 
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.logger = logger;
        this.databaseManager = databaseManager;
    }
    
    // [FIX] แก้ไข Logic ใน GlobalTimer
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

                    if (data.hologramEntity == null || !data.hologramEntity.isValid()) {
                        logger.log(LoggingService.LogLevel.WARN, "โฮโลแกรมของ " + data.ownerName + " หาย! (อาจโดน /kill) ทำการลบกล่อง...");
                        chestsToRemove.add(key);
                        continue; 
                    }

                    // [FIX] 1. เช็กก่อนว่าหมดเวลารึยัง
                    if (data.timeLeft <= 0) {
                        chestsToRemove.add(key); 
                        continue; // หมดเวลา, ไม่ต้องอัปเดต
                    }

                    // [FIX] 2. ถ้ายังไม่หมดเวลา -> อัปเดต Particle (แสดงเวลาปัจจุบัน)
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
                            logger.log(LogLevel.WARN, "เกิดข้อผิดพลาดตอนสร้าง Particle: " + e.getMessage());
                        }
                    }

                    // [FIX] 3. อัปเดต Hologram (แสดงเวลาปัจจุบัน)
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
                    data.hologramEntity.setText(hologramStringBuilder.toString());

                    // [FIX] 4. ลดเวลา (สำหรับ tick หน้า)
                    data.timeLeft--;
                }

                // 5. ลบกล่องที่หมดเวลา
                for (BlockLocation keyToRemove : chestsToRemove) {
                    DeathChestData dataToRemove = activeChests.get(keyToRemove);
                    if (dataToRemove != null) {
                        removeChest(keyToRemove, dataToRemove, true); 
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    // (โค้ดส่วนที่เหลือของไฟล์นี้เหมือนเดิม ไม่ต้องแตะ)
    
    public void staggerLoadChests(List<DatabaseChestData> dbChests) {
        if (dbChests.isEmpty()) {
            return;
        }
        final int CHESTS_PER_TICK = 50; 
        logger.log(LogLevel.INFO, "กำลังทยอยโหลด Active Chests " + dbChests.size() + " กล่อง...");
        new BukkitRunnable() {
            private int processedCount = 0;
            @Override
            public void run() {
                for (int i = 0; i < CHESTS_PER_TICK; i++) {
                    if (dbChests.isEmpty()) {
                        logger.log(LogLevel.INFO, "ทยอยโหลด Active Chests เสร็จสิ้น (" + processedCount + " กล่อง)");
                        this.cancel();
                        return;
                    }
                    DatabaseChestData dbChest = dbChests.remove(0); 
                    processedCount++;
                    try {
                        World world = Bukkit.getWorld(dbChest.world); 
                        if (world == null) {
                            continue;
                        }
                        Location loc = new Location(world, dbChest.x, dbChest.y, dbChest.z);
                        Block block = loc.getBlock();
                        if (block.getType() != Material.CHEST) {
                            if (block.getType() != Material.AIR && !block.isPassable()) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        databaseManager.deleteActiveChest(loc);
                                    }
                                }.runTaskAsynchronously(plugin);
                                continue;
                            }
                            block.setType(Material.CHEST);
                        }
                        if (block.getType() != Material.CHEST) {
                             new BukkitRunnable() {
                                @Override
                                public void run() {
                                    databaseManager.deleteActiveChest(loc);
                                }
                            }.runTaskAsynchronously(plugin);
                             continue;
                        }
                        Chest chest = (Chest) block.getState();
                        ItemStack[] items = SerializationUtils.itemStackArrayFromBase64(dbChest.itemsBase64);
                        UUID ownerUuid = UUID.fromString(dbChest.ownerUuid);
                        String ownerName = dbChest.ownerName;
                        if (ownerName == null || ownerName.equals("Unknown") || ownerName.isEmpty()) {
                             ownerName = ownerUuid.toString();
                        }
                        String locationStr = String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                        Location hologramLoc = loc.clone().add(0.5, configManager.getHologramYOffset(), 0.5);
                        TextDisplay hologram = world.spawn(hologramLoc, TextDisplay.class, (holo) -> {
                            holo.setGravity(false);
                            holo.setPersistent(false);
                            holo.setInvulnerable(true);
                            holo.setBrightness(new Display.Brightness(15, 15));
                            holo.setAlignment(TextDisplay.TextAlignment.CENTER); 
                            holo.setBillboard(Display.Billboard.CENTER);
                            holo.setGlowing(true);
                            holo.setGlowColorOverride(Color.YELLOW);
                            holo.setViewRange(32.0f);
                        });
                        DeathChestData data = new DeathChestData(
                            ownerUuid, ownerName, chest, hologram, items, dbChest.experience,
                            locationStr, dbChest.world, dbChest.x, dbChest.y, dbChest.z, dbChest.createdAt
                        );
                        BlockLocation key = new BlockLocation(data.worldName, data.x, data.y, data.z);
                        activeChests.put(key, data);
                        playerChestMap.putIfAbsent(ownerUuid, new ArrayList<>());
                        playerChestMap.get(ownerUuid).add(key);
                        data.timeLeft = dbChest.remainingSeconds;
                    } catch (Exception e) {
                        logger.log(LogLevel.ERROR, "ไม่สามารถโหลด Active Chest (Staggered): " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public DeathChestData getActiveChestAt(Block block) {
        if (block == null) return null;
        BlockLocation key = new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        return activeChests.get(key);
    }
    
    public DeathChestData getActiveChestAt(BlockLocation key) {
        return activeChests.get(key);
    }

    public DeathChestData getActiveChestAt(Location loc) {
        BlockLocation key = new BlockLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return activeChests.get(key);
    }

    public List<BlockLocation> getActiveChestLocations(UUID playerId) {
        return playerChestMap.getOrDefault(playerId, new ArrayList<>());
    }

    public void createDeathChest(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();
        int totalExp = player.getTotalExperience();
        List<ItemStack> allItems = new ArrayList<>(event.getDrops());
        allItems.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        allItems.add(player.getInventory().getItemInOffHand());
        List<ItemStack> validItems = new ArrayList<>();
        for (ItemStack item : allItems) {
            if (item != null && item.getType() != Material.AIR) {
                validItems.add(item);
            }
        }
        if (totalExp <= 0 && validItems.isEmpty()) {
            event.setDroppedExp(0);
            event.getDrops().clear();
            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0);
            player.getInventory().clear(); 
            logger.log(LoggingService.LogLevel.INFO, "ผู้เล่น " + player.getName() + " ตายแบบตัวเปล่า ไม่สร้างกล่องศพ");
            return;
        }
        event.setDroppedExp(0);
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        if (deathLoc.getBlock().getType() != Material.AIR && deathLoc.getBlock().getType().isOccluding()) {
             deathLoc.setY(deathLoc.getY() + 1);
        }
        Location blockLoc = new Location(deathLoc.getWorld(), deathLoc.getBlockX(), deathLoc.getBlockY(), deathLoc.getBlockZ());
        blockLoc.getBlock().setType(Material.CHEST);
        Chest chest = (Chest) blockLoc.getBlock().getState();
        event.getDrops().clear();
        player.getInventory().clear();
        Location hologramLoc = blockLoc.clone().add(0.5, configManager.getHologramYOffset(), 0.5);
        String locationStr = String.format("%d, %d, %d", blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
        
        TextDisplay hologram = player.getWorld().spawn(hologramLoc, TextDisplay.class, (holo) -> {
            holo.setGravity(false);
            holo.setPersistent(false);
            holo.setInvulnerable(true);
            holo.setBrightness(new Display.Brightness(15, 15));
            holo.setAlignment(TextDisplay.TextAlignment.CENTER); 
            holo.setBillboard(Display.Billboard.CENTER);
            holo.setGlowing(true);
            holo.setGlowColorOverride(Color.YELLOW);
            holo.setViewRange(32.0f); 
        });
        
        long creationTime = System.currentTimeMillis(); 
        DeathChestData data = new DeathChestData(
            player.getUniqueId(), player.getName(), chest, hologram, 
            validItems.toArray(new ItemStack[0]), totalExp, locationStr,
            player.getWorld().getName(), blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ(),
            creationTime
        );

        BlockLocation key = new BlockLocation(data.worldName, data.x, data.y, data.z);
        activeChests.put(key, data);
        
        playerChestMap.putIfAbsent(player.getUniqueId(), new ArrayList<>());
        playerChestMap.get(player.getUniqueId()).add(key); 

        int initialTime = configManager.getDespawnTime();
        data.timeLeft = initialTime;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                databaseManager.saveActiveChest(data, initialTime, creationTime);
            }
        }.runTaskAsynchronously(plugin);

        player.sendMessage(configManager.getChatMessageDeath()
            .replace("&", "§")
            .replace("%coords%", locationStr)
            .replace("%xp%", String.valueOf(totalExp))
        );
        logger.logDeath(player, locationStr, totalExp);
    }

    private void removeChestFromWorld(BlockLocation key, DeathChestData data) {
        if (data == null) {
            data = activeChests.get(key);
            if (data == null) return; 
        }
        if (data.hologramEntity != null && data.hologramEntity.isValid()) {
            data.hologramEntity.remove();
        }
        World world = Bukkit.getWorld(key.worldName());
        if (world != null) {
            Location loc = new Location(world, key.x(), key.y(), key.z());
            if (loc.getBlock().getType() == Material.CHEST) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        activeChests.remove(key);
        List<BlockLocation> playerChests = playerChestMap.get(data.ownerUUID);
        if (playerChests != null) {
            playerChests.remove(key);
        }
    }
    
    private void processChestDatabase(BlockLocation key, DeathChestData data, boolean moveToBuyback) {
        World world = Bukkit.getWorld(key.worldName());
        if (world == null) {
            logger.log(LogLevel.ERROR, "ไม่สามารถลบ Chest จาก DB ได้เพราะโลก '" + key.worldName() + "' ไม่ได้โหลดอยู่");
            return;
        }
        Location loc = new Location(world, key.x(), key.y(), key.z());
        databaseManager.deleteActiveChest(loc);
        if (moveToBuyback) {
            if (data.items.length > 0 || data.experience > 0) {
                storageManager.addBuybackItem(data.ownerUUID, data.items, data.experience);
                logger.logChestExpired(data.ownerName, data.locationString, data.experience);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player owner = Bukkit.getPlayer(data.ownerUUID);
                        if (owner != null && owner.isOnline()) {
                            owner.sendMessage(configManager.getChatMessageExpired().replace("&", "§"));
                        }
                    }
                }.runTask(plugin);
            } else {
                logger.log(LogLevel.INFO, "ลบกล่องศพหมดอายุ (แต่ว่างเปล่า) ของ: " + data.ownerName);
            }
        }
    }

    public void removeChest(BlockLocation key, DeathChestData data, boolean moveToBuyback) {
        if (!Bukkit.isPrimaryThread()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeChestFromWorld(key, data);
                }
            }.runTask(plugin);
        } else {
            removeChestFromWorld(key, data);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                processChestDatabase(key, data, moveToBuyback);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void cleanupAllChests() {
        if (activeChests.isEmpty()) {
            return;
        }
        logger.log(LoggingService.LogLevel.WARN, "กำลังล้างกล่องศพที่ค้างอยู่ " + activeChests.size() + " กล่อง (ก่อนปิดเซิร์ฟ)...");
        for (Map.Entry<BlockLocation, DeathChestData> entry : activeChests.entrySet()) {
            removeChest(entry.getKey(), entry.getValue(), false);
        }
    }

    public void saveAllChestTimes() {
        if (activeChests.isEmpty()) {
            return;
        }
        plugin.getLogger().info("กำลังเซฟเวลาที่เหลือของ Active Chests " + activeChests.size() + " กล่อง (แบบ Batch)...");
        databaseManager.batchUpdateChestTimes(activeChests);
    }

    public void cleanupEntitiesOnDisable() {
        if (activeChests.isEmpty()) {
            return;
        }
        plugin.getLogger().info("กำลังลบ Holograms " + activeChests.size() + " อัน (ตอนปิดเซิร์ฟ)...");
        for (DeathChestData data : activeChests.values()) {
            if (data.hologramEntity != null && data.hologramEntity.isValid()) {
                data.hologramEntity.remove();
            }
        }
        activeChests.clear();
        playerChestMap.clear();
    }

    private Particle resolveParticle(String particleName, Particle fallback) {
        try {
            return Particle.valueOf(particleName);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}