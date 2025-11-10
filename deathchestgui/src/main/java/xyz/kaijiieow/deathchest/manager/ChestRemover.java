package xyz.kaijiieow.deathchest.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.database.ChestDatabase;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.manager.StorageManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChestRemover {
    
    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final LoggingService logger;
    private final ChestDatabase chestDatabase;
    private final StorageManager storageManager;
    private final Map<BlockLocation, DeathChestData> activeChests;
    private final Map<UUID, List<BlockLocation>> playerChestMap;
    
    public ChestRemover(DeathChestPlugin plugin, ConfigManager configManager, LoggingService logger,
                       ChestDatabase chestDatabase, StorageManager storageManager,
                       Map<BlockLocation, DeathChestData> activeChests,
                       Map<UUID, List<BlockLocation>> playerChestMap) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
        this.chestDatabase = chestDatabase;
        this.storageManager = storageManager;
        this.activeChests = activeChests;
        this.playerChestMap = playerChestMap;
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
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถลบ Chest จาก DB ได้เพราะโลก '" + key.worldName() + "' ไม่ได้โหลดอยู่");
            return;
        }
        Location loc = new Location(world, key.x(), key.y(), key.z());
        chestDatabase.deleteActiveChest(loc);
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
                logger.log(LoggingService.LogLevel.INFO, "ลบกล่องศพหมดอายุ (แต่ว่างเปล่า) ของ: " + data.ownerName);
            }
        }
    }
}

