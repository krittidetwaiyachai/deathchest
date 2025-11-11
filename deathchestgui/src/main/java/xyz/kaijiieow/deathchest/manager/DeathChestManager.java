package xyz.kaijiieow.deathchest.manager;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.model.DatabaseChestData;
import xyz.kaijiieow.deathchest.database.ChestDatabase;
import xyz.kaijiieow.deathchest.database.DatabaseManager;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.manager.StorageManager;
import xyz.kaijiieow.deathchest.manager.HookManager;

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
    private final ChestDatabase chestDatabase;
    private final HookManager hookManager;

    private final Map<BlockLocation, DeathChestData> activeChests = new ConcurrentHashMap<>();
    private final Map<UUID, List<BlockLocation>> playerChestMap = new ConcurrentHashMap<>();
    
    private final Particle particleSoulFireFlame = resolveParticle("SOUL_FIRE_FLAME", Particle.FLAME);
    private final Particle particleElectricSpark = resolveParticle("ELECTRIC_SPARK", Particle.FIREWORKS_SPARK);
    private final Particle particleSculkSoul = resolveParticle("SCULK_SOUL", Particle.PORTAL);

    private final StringBuilder hologramStringBuilder = new StringBuilder();
    
    private ChestTimer chestTimer;
    private ChestLoader chestLoader;
    private ChestRemover chestRemover;

    public DeathChestManager(DeathChestPlugin plugin, ConfigManager configManager, StorageManager storageManager, LoggingService logger, DatabaseManager databaseManager, HookManager hookManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.logger = logger;
        this.databaseManager = databaseManager;
        this.chestDatabase = databaseManager.getChestDatabase();
        this.hookManager = hookManager;
        
        this.chestRemover = new ChestRemover(plugin, configManager, logger, chestDatabase, storageManager, activeChests, playerChestMap);
        this.chestTimer = new ChestTimer(plugin, configManager, logger, activeChests, chestRemover, particleSoulFireFlame, particleElectricSpark, particleSculkSoul);
        this.chestLoader = new ChestLoader(plugin, configManager, logger, chestDatabase, activeChests, playerChestMap);
    }
    
    public void startGlobalTimer() {
        chestTimer.startGlobalTimer();
    }
    
    public void staggerLoadChests(List<DatabaseChestData> dbChests) {
        chestLoader.staggerLoadChests(dbChests);
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
        
        // เช็ค keepInventory gamerule - ถ้าเปิดอยู่จะไม่สร้างกล่องศพ
        if (player.getWorld().getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY)) {
            logger.log(LoggingService.LogLevel.INFO, "ผู้เล่น " + player.getName() + " ตายแต่ keepInventory เปิดอยู่ ไม่สร้างกล่องศพ");
            return;
        }
        
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
        
        // ใช้เวลา despawn ตามยศของผู้เล่นที่ตาย
        int initialTime;
        if (configManager.getDespawnTimerByGroup().isEmpty()) {
            initialTime = configManager.getDespawnTime();
        } else {
            String playerGroup = hookManager.getPlayerGroup(player);
            initialTime = configManager.getDespawnTimeForGroup(playerGroup);
        }
        int minutes = initialTime / 60;
        int seconds = initialTime % 60;
        final String timeString;
        if (minutes > 0) {
            timeString = minutes + " นาที " + seconds;
        } else {
            timeString = String.valueOf(initialTime);
        }
        
        hologramStringBuilder.setLength(0);
        List<String> lines = configManager.getHologramLines();
        for (int j = 0; j < lines.size(); j++) {
            String line = lines.get(j);
            line = line.replace("&", "§")
                    .replace("%player%", player.getName())
                    .replace("%time%", timeString)
                    .replace("%xp%", String.valueOf(totalExp))
                    .replace("%coords%", locationStr);
            
            hologramStringBuilder.append(line);
            if (j < lines.size() - 1) {
                hologramStringBuilder.append("\n");
            }
        }
        final String initialText = hologramStringBuilder.toString();
        
        TextDisplay hologram = player.getWorld().spawn(hologramLoc, TextDisplay.class, (holo) -> {
            holo.setGravity(false);
            holo.setPersistent(false);
            holo.setInvulnerable(true);
            holo.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            holo.setAlignment(TextDisplay.TextAlignment.CENTER);
            holo.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            holo.setGlowing(true);
            holo.setGlowColorOverride(Color.YELLOW);
            holo.setViewRange(64.0f);
            holo.setText(initialText);
        });
        
        long creationTime = System.currentTimeMillis();
        DeathChestData data = new DeathChestData(
            player.getUniqueId(), player.getName(), chest, hologram,
            validItems.toArray(new ItemStack[0]), totalExp, locationStr,
            player.getWorld().getName(), blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ(),
            creationTime, initialTime
        );
        BlockLocation key = new BlockLocation(data.worldName, data.x, data.y, data.z);
        activeChests.put(key, data);
        playerChestMap.putIfAbsent(player.getUniqueId(), new ArrayList<>());
        playerChestMap.get(player.getUniqueId()).add(key);
        
        data.timeLeft = initialTime;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                chestDatabase.saveActiveChest(data, initialTime, creationTime);
            }
        }.runTaskAsynchronously(plugin);
        player.sendMessage(configManager.getChatMessageDeath()
            .replace("&", "§")
            .replace("%coords%", locationStr)
            .replace("%xp%", String.valueOf(totalExp))
        );
        logger.logDeath(player, locationStr, totalExp);
    }
    
    public void removeChest(BlockLocation key, DeathChestData data, boolean moveToBuyback) {
        chestRemover.removeChest(key, data, moveToBuyback);
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
        chestDatabase.batchUpdateChestTimes(activeChests);
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

