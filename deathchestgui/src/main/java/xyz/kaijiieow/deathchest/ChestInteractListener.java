package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestInteractListener implements Listener {

    private final DeathChestPlugin plugin;
    private final DeathChestManager deathChestManager;
    private final ConfigManager configManager;
    
    // [NEW] แมพสำหรับเก็บว่าใครกำลังเปิดกล่องเสมือนของที่ไหน
    private final Map<UUID, Location> viewingChest = new HashMap<>();
    private final String VIRTUAL_CHEST_TITLE_PREFIX = "กล่องศพของ ";

    public ChestInteractListener(DeathChestPlugin plugin, DeathChestManager deathChestManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.deathChestManager = deathChestManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CHEST) {
            return;
        }

        Location loc = event.getClickedBlock().getLocation();
        DeathChestData data = deathChestManager.getActiveChestAt(loc);

        if (data == null) {
            return; // ไม่ใช่กล่องศพของเรา
        }

        // [NEW] ยกเลิกการเปิดกล่องจริง (3 แถว) ทันที!
        event.setCancelled(true);

        Player player = event.getPlayer();
        boolean isOwner = player.getUniqueId().equals(data.ownerUUID);

        if (!isOwner && !configManager.isAllowOtherPlayersToOpen()) {
            player.sendMessage(configManager.getChatMessageNotYourChest().replace("&", "§"));
            return;
        }

        if (isOwner && data.experience > 0) {
            player.giveExp(data.experience);
            player.sendMessage(configManager.getChatMessageXpRestored().replace("&", "§").replace("%xp%", String.valueOf(data.experience)));
            data.experience = 0;
        }

        // [NEW] สร้าง GUI 6 แถว (54 ช่อง) เสมือนขึ้นมา
        Inventory virtualChest = Bukkit.createInventory(player, 54, VIRTUAL_CHEST_TITLE_PREFIX + data.ownerName);
        
        // [NEW] ยัดของจาก Data (ไม่ใช่จากกล่องจริง) ลงไป
        if (data.items != null) {
            virtualChest.setContents(data.items);
        }

        // [NEW] เปิด GUI เสมือน
        player.openInventory(virtualChest);
        
        // [NEW] บันทึกว่ามึงกำลังดูกล่องนี้
        viewingChest.put(player.getUniqueId(), loc);
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        
        // [NEW] เช็คว่าใช่ GUI เสมือนของเรารึเปล่า
        Location loc = viewingChest.remove(player.getUniqueId());
        if (loc == null) {
            return; // ไม่ใช่กล่องเสมือนของเรา
        }

        DeathChestData data = deathChestManager.getActiveChestAt(loc);
        if (data == null) {
            return; // กล่องหายไปแล้ว?
        }
        
        // [NEW] เช็ค Title อีกรอบ กันเหนียว
        if (!event.getView().getTitle().equals(VIRTUAL_CHEST_TITLE_PREFIX + data.ownerName)) {
            return;
        }

        // [NEW] อัปเดตของที่เหลือใน Data
        ItemStack[] newContents = event.getInventory().getContents();
        data.items = newContents;

        // [NEW] เช็คว่ากล่องว่างรึเปล่า
        boolean isEmpty = true;
        for (ItemStack item : newContents) {
            if (item != null && item.getType() != Material.AIR) {
                isEmpty = false;
                break;
            }
        }

        // [NEW] ถ้าว่าง (และ XP ก็ 0 ไปแล้ว) -> ลบกล่องทิ้ง (ไม่ย้ายไป buyback)
        if (isEmpty && data.experience == 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // (ต้องรัน task ถัดไป ไม่งั้นมันพยายามลบกล่องขณะที่ยัง "ปิด" ไม่เสร็จ)
                    
                    // [NEW] Log to Discord
                    plugin.getLoggingService().logChestCollected(data.ownerName, data.locationString);
                    
                    deathChestManager.removeChest(loc, data, false); 
                }
            }.runTask(plugin);
        }
    }
}