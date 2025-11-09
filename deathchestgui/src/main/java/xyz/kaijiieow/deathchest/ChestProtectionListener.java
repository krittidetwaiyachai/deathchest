package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
// import org.bukkit.Location; // [FIX 3.2] ไม่ใช้แล้ว
import org.bukkit.Material;
import org.bukkit.block.Block; // [FIX 3.2]
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
    
    // [FIX 3.2] เปลี่ยน Map value จาก Location เป็น BlockLocation
    private final Map<UUID, BlockLocation> viewingChest = new HashMap<>();
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

        Block block = event.getClickedBlock(); // [FIX 3.2]
        DeathChestData data = deathChestManager.getActiveChestAt(block); // [FIX 3.2] ใช้เมธอดใหม่

        if (data == null) {
            return; 
        }

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

        Inventory virtualChest = Bukkit.createInventory(player, 54, VIRTUAL_CHEST_TITLE_PREFIX + data.ownerName);
        
        if (data.items != null) {
            virtualChest.setContents(data.items);
        }

        player.openInventory(virtualChest);
        
        // [FIX 3.2] สร้างและเก็บ Key (BlockLocation)
        BlockLocation key = new BlockLocation(data.worldName, data.x, data.y, data.z);
        viewingChest.put(player.getUniqueId(), key);
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        
        // [FIX 3.2] ดึง Key (BlockLocation) ออกมา
        BlockLocation key = viewingChest.remove(player.getUniqueId());
        if (key == null) {
            return; 
        }

        // [FIX 3.2] ใช้ Key (BlockLocation) ในการดึงข้อมูล
        DeathChestData data = deathChestManager.getActiveChestAt(key);
        if (data == null) {
            return; 
        }
        
        if (!event.getView().getTitle().equals(VIRTUAL_CHEST_TITLE_PREFIX + data.ownerName)) {
            return;
        }

        ItemStack[] newContents = event.getInventory().getContents();
        data.items = newContents;

        boolean isEmpty = true;
        for (ItemStack item : newContents) {
            if (item != null && item.getType() != Material.AIR) {
                isEmpty = false;
                break;
            }
        }

        if (isEmpty && data.experience == 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getLoggingService().logChestCollected(data.ownerName, data.locationString);
                    // [FIX 3.2] ส่ง Key (BlockLocation) ไปลบ
                    deathChestManager.removeChest(key, data, false); 
                }
            }.runTask(plugin);
        }
    }
}