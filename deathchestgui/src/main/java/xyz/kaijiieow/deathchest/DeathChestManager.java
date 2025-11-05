package xyz.kaijiieow.deathchest;

// import eu.decentsoftware.holograms.api.DHAPI;
// import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.shared.DecentHologramsException;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// จัดการ Logic การตาย, สร้างกล่อง, จับเวลา
public class DeathChestManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final HookManager hookManager;
    private final StorageManager storageManager;
    private final LoggingService logger;

    private final Map<Location, DeathChestData> activeChests = new HashMap<>();

    public DeathChestManager(DeathChestPlugin plugin, ConfigManager configManager, HookManager hookManager, StorageManager storageManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.hookManager = hookManager;
        this.storageManager = storageManager;
        this.logger = logger;
    }

    public void createDeathChest(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        // (ควรเพิ่ม Logic หาที่ปลอดภัยวางกล่อง)
        if (deathLoc.getBlock().getType() != Material.AIR && deathLoc.getBlock().getType().isOccluding()) {
             deathLoc.setY(deathLoc.getY() + 1);
        }

        deathLoc.getBlock().setType(Material.CHEST);
        Chest chest = (Chest) deathLoc.getBlock().getState();

        // ย้ายของ
        List<ItemStack> allItems = new ArrayList<>(event.getDrops());
        allItems.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        
        for (ItemStack item : allItems) {
            if (item != null && item.getType() != Material.AIR) {
                chest.getBlockInventory().addItem(item);
            }
        }
        
        event.getDrops().clear();

        // สร้าง Hologram
        String hologramName = "DeathChest-" + player.getUniqueId() + "-" + System.currentTimeMillis();
        Location hologramLoc = deathLoc.clone().add(0.5, 2.0, 0.5);
        Hologram hologram = DHAPI.createHologram(hologramName, hologramLoc);
        
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
                if (!activeChests.containsKey(loc) || timeLeft <= 0) {
                    this.cancel();
                    if (activeChests.containsKey(loc)) {
                        logger.log(LoggingService.LogLevel.INFO, "กล่องศพของ " + data.ownerName + " หมดเวลา (ย้ายไป buyback)");
                        removeChest(loc, data, true); // true = ย้ายไปที่คลังซื้อคืน
                    }
                    return;
                }

                // อัปเดต Hologram
                List<String> lines = configManager.getHologramLines().stream()
                        .map(line -> line.replace("&", "§")
                                .replace("%player%", data.ownerName)
                                .replace("%time%", String.valueOf(timeLeft)))
                        .collect(Collectors.toList());
                DHAPI.updateHologram(data.hologram.getName(), lines);

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void removeChest(Location loc, DeathChestData data, boolean moveToBuyback) {
        if (data.hologram != null) {
            data.hologram.delete();
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
}