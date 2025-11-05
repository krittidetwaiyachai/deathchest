package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeathChestManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final LoggingService logger;

    private final Map<Location, DeathChestData> activeChests = new HashMap<>();

    public DeathChestManager(DeathChestPlugin plugin, ConfigManager configManager, StorageManager storageManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.logger = logger;
    }

    public void createDeathChest(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        if (deathLoc.getBlock().getType() != Material.AIR && deathLoc.getBlock().getType().isOccluding()) {
             deathLoc.setY(deathLoc.getY() + 1);
        }

        deathLoc.getBlock().setType(Material.CHEST);
        Chest chest = (Chest) deathLoc.getBlock().getState();

        List<ItemStack> allItems = new ArrayList<>(event.getDrops());
        allItems.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        
        for (ItemStack item : allItems) {
            if (item != null && item.getType() != Material.AIR) {
                chest.getBlockInventory().addItem(item);
            }
        }
        
        event.getDrops().clear();

        Location hologramLoc = deathLoc.clone().add(0.5, 1.5, 0.5);
        
        TextDisplay hologram = player.getWorld().spawn(hologramLoc, TextDisplay.class, (holo) -> {
            holo.setGravity(false);
            holo.setPersistent(false);
            holo.setInvulnerable(true);
            holo.setBrightness(new Display.Brightness(15, 15));
            holo.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // แก้แล้ว
            holo.setAlignment(TextDisplay.Alignment.CENTER); // แก้แล้ว
        });
        
        DeathChestData data = new DeathChestData(player.getUniqueId(), player.getName(), chest, hologram, allItems.toArray(new ItemStack[0]));
        activeChests.put(deathLoc, data);
        startDespawnTimer(deathLoc, data);

        String locationStr = deathLoc.getBlockX() + ", " + deathLoc.getBlockY() + ", " + deathLoc.getBlockZ();
        player.sendMessage("§cคุณตาย! ของของคุณอยู่ในกล่องที่ตำแหน่ง: " + locationStr);
        logger.log(LoggingService.LogLevel.INFO, "สร้างกล่องศพให้ " + player.getName() + " ที่ " + locationStr);
    }

    private void startDespawnTimer(Location loc, DeathChestData data) {
        new BukkitRunnable() {
            int timeLeft = configManager.getDespawnTime();

            @Override
            public void run() {
                if (!activeChests.containsKey(loc) || data.hologramEntity == null || !data.hologramEntity.isValid()) {
                    this.cancel();
                    if (activeChests.containsKey(loc)) {
                        logger.log(LoggingService.LogLevel.INFO, "โฮโลแกรมของ " + data.ownerName + " หาย! (อาจโดน /kill) ทำการลบกล่อง...");
                        removeChest(loc, data, true);
                    }
                    return;
                }

                if (timeLeft <= 0) {
                    this.cancel();
                    logger.log(LoggingService.LogLevel.INFO, "กล่องศพของ " + data.ownerName + " หมดเวลา (ย้ายไป buyback)");
                    removeChest(loc, data, true); 
                    return;
                }

                String text = configManager.getHologramLines().stream()
                        .map(line -> line.replace("&", "§")
                                .replace("%player%", data.ownerName)
                                .replace("%time%", String.valueOf(timeLeft)))
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
            ((Chest) loc.getBlock().getState()).getInventory().clear();
            loc.getBlock().setType(Material.AIR);
        }
        
        activeChests.remove(loc);

        if (moveToBuyback) {
            storageManager.addLostItems(data.ownerUUID, data.items);
            
            Player owner = Bukkit.getPlayer(data.ownerUUID);
            if (owner != null && owner.isOnline()) {
                owner.sendMessage("§cกล่องเก็บของของคุณหมดเวลา! §eคุณสามารถซื้อคืนได้ด้วยคำสั่ง §f/buyback");
            }
        } else {
            logger.log(LoggingService.LogLevel.INFO, "ลบกล่องศพของ " + data.ownerName + " (ถูกเก็บ/เคลียร์)");
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