package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeathChestManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final LoggingService logger;

    private final Map<Location, DeathChestData> activeChests = new HashMap<>();
    private final Map<UUID, Location> playerChestMap = new HashMap<>(); 

    public DeathChestManager(DeathChestPlugin plugin, ConfigManager configManager, StorageManager storageManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.logger = logger;
    }

    public DeathChestData getActiveChestAt(Location loc) {
        // [FIX] สร้าง Key แบบบังคับเป็น Block Location (ตัดทศนิยม, yaw, pitch ทิ้ง)
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return activeChests.get(blockLoc);
    }

    public Location getPlayerChestLocation(UUID playerId) {
        return playerChestMap.get(playerId);
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

        if (totalExp == 0 && validItems.isEmpty()) {
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

        // [FIX] สร้าง Key แบบบังคับเป็น Block Location
        Location blockLoc = new Location(deathLoc.getWorld(), deathLoc.getBlockX(), deathLoc.getBlockY(), deathLoc.getBlockZ());

        blockLoc.getBlock().setType(Material.CHEST);
        Chest chest = (Chest) blockLoc.getBlock().getState();

        event.getDrops().clear();
        player.getInventory().clear();

        // [FIX] ใช้ blockLoc.clone() (ที่ไม่มีทศนิยม) เป็นฐาน
        Location hologramLoc = blockLoc.clone().add(0.5, configManager.getHologramYOffset(), 0.5);
        String locationStr = String.format("%d, %d, %d", blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
        
        TextDisplay hologram = player.getWorld().spawn(hologramLoc, TextDisplay.class, (holo) -> {
            holo.setGravity(false);
            holo.setPersistent(false);
            holo.setInvulnerable(true);
            holo.setBrightness(new Display.Brightness(15, 15));
            holo.setAlignment(TextDisplay.TextAlignment.CENTER); // [FIX] มึงใช้ TextAlignment.CENTER มาตลอด มันผิด
            holo.setBillboard(Display.Billboard.CENTER);
        });
        
        DeathChestData data = new DeathChestData(
            player.getUniqueId(), 
            player.getName(), 
            chest, 
            hologram, 
            validItems.toArray(new ItemStack[0]),
            totalExp,
            locationStr
        );

        // [FIX] ใช้ Block Location เป็น Key
        activeChests.put(blockLoc, data);
        playerChestMap.put(player.getUniqueId(), blockLoc); 
        // [FIX] ส่ง Block Location ไปให้ Timer
        startDespawnTimer(blockLoc, data);

        player.sendMessage(configManager.getChatMessageDeath()
            .replace("&", "§")
            .replace("%coords%", locationStr)
            .replace("%xp%", String.valueOf(totalExp))
        );
        
        logger.logDeath(player, locationStr, totalExp);
    }

    private void startDespawnTimer(Location loc, DeathChestData data) {
        new BukkitRunnable() {
            int timeLeft = configManager.getDespawnTime();

            @Override
            public void run() {
                // [FIX] ต้องใช้ loc (ที่เป็น Block Location อยู่แล้ว) ในการเช็ค
                if (!activeChests.containsKey(loc) || data.hologramEntity == null || !data.hologramEntity.isValid()) {
                    this.cancel();
                    if (activeChests.containsKey(loc)) {
                        logger.log(LoggingService.LogLevel.WARN, "โฮโลแกรมของ " + data.ownerName + " หาย! (อาจโดน /kill) ทำการลบกล่อง...");
                        removeChest(loc, data, true);
                    }
                    return;
                }

                if (timeLeft <= 0) {
                    this.cancel();
                    // [EDIT] เปลี่ยน log ธรรมดาไปเรียกใน removeChest
                    // logger.log(LoggingService.LogLevel.INFO, "กล่องศพของ " + data.ownerName + " หมดเวลา (ย้ายไป buyback)");
                    removeChest(loc, data, true); 
                    return;
                }

                String text = configManager.getHologramLines().stream()
                        .map(line -> line.replace("&", "§")
                                .replace("%player%", data.ownerName)
                                .replace("%time%", String.valueOf(timeLeft))
                                .replace("%xp%", String.valueOf(data.experience))
                                .replace("%coords%", data.locationString)
                        )
                        .collect(Collectors.joining("\n"));
                
                data.hologramEntity.setText(text);

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void removeChest(Location loc, DeathChestData data, boolean moveToBuyback) {
        if (data.hologramEntity != null && data.hologramEntity.isValid()) {
            data.hologramEntity.remove();
        }

        // [FIX] ใช้ loc (ที่เป็น Block Location อยู่แล้ว)
        if (loc.getBlock().getType() == Material.CHEST) {
            loc.getBlock().setType(Material.AIR);
        }
        
        activeChests.remove(loc);
        playerChestMap.remove(data.ownerUUID); 

        if (moveToBuyback && (data.items.length > 0 || data.experience > 0)) {
            DeathDataPackage dataPackage = new DeathDataPackage(data.items, data.experience);
            storageManager.addLostItems(data.ownerUUID, dataPackage);
            
            // [NEW] Log to Discord
            logger.logChestExpired(data.ownerName, data.locationString, data.experience);

            Player owner = Bukkit.getPlayer(data.ownerUUID);
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(configManager.getChatMessageExpired().replace("&", "§"));
            }
        } else if (moveToBuyback) {
            // [NEW] Case: Chest expired but was empty
            logger.log(LogLevel.INFO, "ลบกล่องศพหมดอายุ (แต่ว่างเปล่า) ของ: " + data.ownerName);
        } else {
            // [NEW] Case: Chest was collected (moveToBuyback = false)
            // Logging for this is handled by ChestInteractListener.
            // We don't log anything here to avoid duplicates.
        }
    }

    public void cleanupAllChests() {
        if (activeChests.isEmpty()) {
            return;
        }
        logger.log(LoggingService.LogLevel.WARN, "กำลังล้างกล่องศพที่ค้างอยู่ " + activeChests.size() + " กล่อง (ก่อนปิดเซิร์ฟ)...");
        for (Map.Entry<Location, DeathChestData> entry : new ArrayList<>(activeChests.entrySet())) {
            removeChest(entry.getKey(), entry.getValue(), false);
        }
    }
}