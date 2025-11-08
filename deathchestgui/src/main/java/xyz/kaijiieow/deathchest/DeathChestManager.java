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
    private final Map<UUID, List<Location>> playerChestMap = new HashMap<>(); 

    public DeathChestManager(DeathChestPlugin plugin, ConfigManager configManager, StorageManager storageManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.logger = logger;
    }

    public DeathChestData getActiveChestAt(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return activeChests.get(blockLoc);
    }

    public List<Location> getActiveChestLocations(UUID playerId) {
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

        // [EDIT] แก้บัค: ถ้า XP เป็น 0 (หรือน้อยกว่า) และ ไม่มีของ ถึงจะไม่สร้าง
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

        activeChests.put(blockLoc, data);
        
        playerChestMap.putIfAbsent(player.getUniqueId(), new ArrayList<>());
        playerChestMap.get(player.getUniqueId()).add(blockLoc); 

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

        if (loc.getBlock().getType() == Material.CHEST) {
            loc.getBlock().setType(Material.AIR);
        }
        
        activeChests.remove(loc);
        
        List<Location> playerChests = playerChestMap.get(data.ownerUUID);
        if (playerChests != null) {
            playerChests.remove(loc);
        }

        // [CHECK] ส่วนนี้ถูกต้องอยู่แล้ว (มันเก็บ XP ไป buyback ถ้า XP > 0)
        if (moveToBuyback && (data.items.length > 0 || data.experience > 0)) {
            DeathDataPackage dataPackage = new DeathDataPackage(data.items, data.experience);
            storageManager.addLostItems(data.ownerUUID, dataPackage);
            
            logger.logChestExpired(data.ownerName, data.locationString, data.experience);

            Player owner = Bukkit.getPlayer(data.ownerUUID);
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(configManager.getChatMessageExpired().replace("&", "§"));
            }
        } else if (moveToBuyback) {
            logger.log(LogLevel.INFO, "ลบกล่องศพหมดอายุ (แต่ว่างเปล่า) ของ: " + data.ownerName);
        } else {
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