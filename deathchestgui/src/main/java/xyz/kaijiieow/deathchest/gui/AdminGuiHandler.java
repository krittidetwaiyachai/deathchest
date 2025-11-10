package xyz.kaijiieow.deathchest.gui;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.manager.DeathChestManager;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.model.DeathDataPackage;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.manager.StorageManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminGuiHandler {
    
    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final LoggingService logger;
    
    public static final String ADMIN_GUI_TITLE_PREFIX = "§c[Admin] กล่องของ ";
    private static final int ITEMS_PER_PAGE = 45;
    
    private final ItemStack NEXT_PAGE_ITEM;
    private final ItemStack PREV_PAGE_ITEM;
    
    public AdminGuiHandler(DeathChestPlugin plugin, ConfigManager configManager, StorageManager storageManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.logger = logger;
        
        NEXT_PAGE_ITEM = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta nextMeta = NEXT_PAGE_ITEM.getItemMeta();
        nextMeta.setDisplayName("§aหน้าถัดไป -->");
        NEXT_PAGE_ITEM.setItemMeta(nextMeta);

        PREV_PAGE_ITEM = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta prevMeta = PREV_PAGE_ITEM.getItemMeta();
        prevMeta.setDisplayName("§c<-- หน้าก่อนหน้า");
        PREV_PAGE_ITEM.setItemMeta(prevMeta);
    }
    
    public void openAdminChestGUI(Player admin, OfflinePlayer targetPlayer, int page, Map<UUID, Integer> playerPages, Map<UUID, UUID> adminViewing) {
        UUID targetUUID = targetPlayer.getUniqueId();
        adminViewing.put(admin.getUniqueId(), targetUUID);
        playerPages.put(admin.getUniqueId(), page);

        List<BlockLocation> chestLocations = plugin.getDeathChestManager().getActiveChestLocations(targetUUID);
        List<DeathDataPackage> buybackItems = storageManager.getLostItems(targetUUID);

        int totalActiveChests = (chestLocations != null) ? chestLocations.size() : 0;
        int totalBuybackItems = (buybackItems != null) ? buybackItems.size() : 0;
        int totalItems = totalActiveChests + totalBuybackItems;

        if (totalItems == 0) {
            admin.sendMessage(configManager.getChatMessageAdminNoChests()
                .replace("&", "§")
                .replace("%player%", targetPlayer.getName()));
            return;
        }

        int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (maxPage == 0) maxPage = 1;

        String guiTitle = String.format("%s%s (หน้า %d/%d)", 
            ADMIN_GUI_TITLE_PREFIX, 
            targetPlayer.getName(), 
            page + 1, 
            maxPage
        );
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;

            if (i < totalActiveChests) {
                BlockLocation key = chestLocations.get(i);
                DeathChestData data = plugin.getDeathChestManager().getActiveChestAt(key);

                if (data == null) continue;

                ItemStack guiItem = new ItemStack(Material.CHEST);
                ItemMeta meta = guiItem.getItemMeta();
                meta.setDisplayName("§c[Active] กล่องศพที่: " + data.locationString);
                meta.setLore(Arrays.asList(
                    "§7เจ้าของ: " + data.ownerName,
                    "§bXP: " + data.experience,
                    "",
                    "§eคลิกเพื่อวาร์ป"
                ));
                guiItem.setItemMeta(meta);
                gui.setItem(slot, guiItem);
            } else {
                int buybackIndex = i - totalActiveChests;
                DeathDataPackage dataPackage = buybackItems.get(buybackIndex);

                ItemStack guiItem = new ItemStack(Material.ENDER_CHEST);
                ItemMeta meta = guiItem.getItemMeta();
                meta.setDisplayName("§d[Buyback] ชุดของ (Set " + (buybackIndex + 1) + ")");
                
                int totalItemCount = 0;
                if (dataPackage.getItems() != null) {
                    for(ItemStack it : dataPackage.getItems()) {
                        if(it != null) totalItemCount += it.getAmount();
                    }
                }
                
                meta.setLore(Arrays.asList(
                    "§7จำนวน: " + totalItemCount + " ชิ้น",
                    "§bXP: " + dataPackage.getExperience(),
                    "",
                    "§8(ไม่สามารถวาร์ปได้)"
                ));
                guiItem.setItemMeta(meta);
                gui.setItem(slot, guiItem);
            }
        }

        if (page > 0) {
            gui.setItem(45, PREV_PAGE_ITEM);
        }
        if (endIndex < totalItems) {
            gui.setItem(53, NEXT_PAGE_ITEM);
        }

        admin.openInventory(gui);
    }
    
    public void handleAdminGuiClick(Player admin, int slot, ItemStack clickedItem, int currentPage, Map<UUID, UUID> adminViewing) {
        if (clickedItem.getType() != Material.CHEST && clickedItem.getType() != Material.ENDER_CHEST) {
            return;
        }

        UUID targetUUID = adminViewing.get(admin.getUniqueId());
        if (targetUUID == null) {
            admin.closeInventory();
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID);

        int index = (currentPage * ITEMS_PER_PAGE) + slot;
        
        List<BlockLocation> chestLocations = plugin.getDeathChestManager().getActiveChestLocations(targetUUID);
        int totalActiveChests = (chestLocations != null) ? chestLocations.size() : 0;

        if (index < totalActiveChests) {
            if (index >= chestLocations.size()) {
                admin.sendMessage("§cเกิดข้อผิดพลาด: ไม่พบรายการกล่อง (Index: " + index + ")");
                admin.closeInventory();
                return;
            }

            BlockLocation targetKey = chestLocations.get(index);
            World world = Bukkit.getWorld(targetKey.worldName());
            if (world == null) {
                admin.sendMessage("§cเกิดข้อผิดพลาด: ไม่สามารถหาโลกของกล่องศพได้ (โลกอาจจะยังไม่ได้โหลด)");
                admin.closeInventory();
                return;
            }

            DeathChestData data = plugin.getDeathChestManager().getActiveChestAt(targetKey);
            String locationString = (data != null) ? data.locationString : "N/A";
            
            Location safeLoc = new Location(world, targetKey.x() + 0.5, targetKey.y() + 1.0, targetKey.z() + 0.5);
            
            admin.teleport(safeLoc);
            admin.sendMessage(configManager.getChatMessageAdminTeleported().replace("&", "§"));
            
            logger.logAdminTpSuccess(admin, targetPlayer, locationString);
            
            admin.closeInventory();
        } else {
            int buybackIndex = index - totalActiveChests;
            
            logger.logAdminTpFailBuyback(admin, targetPlayer, buybackIndex);

            admin.sendMessage(configManager.getChatMessageAdminIsBuyback().replace("&", "§"));
        }
    }
    
    public ItemStack getNextPageItem() {
        return NEXT_PAGE_ITEM;
    }
    
    public ItemStack getPrevPageItem() {
        return PREV_PAGE_ITEM;
    }
}

