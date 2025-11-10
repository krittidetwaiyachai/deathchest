package xyz.kaijiieow.deathchest.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.manager.DeathChestManager;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestInteractListener implements Listener {

    private final DeathChestPlugin plugin;
    private final DeathChestManager deathChestManager;
    private final ConfigManager configManager;
    
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

        Block block = event.getClickedBlock();
        DeathChestData data = deathChestManager.getActiveChestAt(block);

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
        
        BlockLocation key = new BlockLocation(data.worldName, data.x, data.y, data.z);
        viewingChest.put(player.getUniqueId(), key);
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        
        BlockLocation key = viewingChest.remove(player.getUniqueId());
        if (key == null) {
            return;
        }

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
                    deathChestManager.removeChest(key, data, false);
                }
            }.runTask(plugin);
        }
    }
}

