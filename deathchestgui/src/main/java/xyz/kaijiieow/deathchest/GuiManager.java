package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final HookManager hookManager;
    private final StorageManager storageManager;
    private final LoggingService logger;

    public static final String GUI_TITLE_PREFIX = "§4[Buyback] - ซื้อของคืน";
    public static final String ADMIN_GUI_TITLE_PREFIX = "§c[Admin] กล่องของ ";
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, UUID> adminViewing = new HashMap<>(); // Admin UUID -> Target UUID
    private final ItemStack NEXT_PAGE_ITEM;
    private final ItemStack PREV_PAGE_ITEM;
    private static final int ITEMS_PER_PAGE = 45;

    public GuiManager(DeathChestPlugin plugin, ConfigManager configManager, HookManager hookManager, StorageManager storageManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.hookManager = hookManager;
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

    public UUID getAdminViewing(UUID adminId) {
        return adminViewing.get(adminId);
    }

    public void openBuybackGUI(Player player) {
        openBuybackGUI(player, 0);
    }

    public void openBuybackGUI(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
        List<DeathDataPackage> playerLostItems = storageManager.getLostItems(player.getUniqueId());

        if (playerLostItems == null || playerLostItems.isEmpty()) {
            player.sendMessage("§eคุณไม่มีรายการของที่สามารถซื้อคืนได้");
            return;
        }

        int totalItems = playerLostItems.size();
        int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (maxPage == 0) maxPage = 1;

        String guiTitle = String.format("%s (หน้า %d/%d)", GUI_TITLE_PREFIX, page + 1, maxPage);
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);

        int currencyCost = configManager.getBuybackCost();
        String currencyName = hookManager.getActiveCurrencyName();

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            DeathDataPackage dataPackage = playerLostItems.get(i);

            ItemStack guiItem = new ItemStack(Material.CHEST);
            ItemMeta meta = guiItem.getItemMeta();
            meta.setDisplayName("§cของที่หายไป (Set " + (i + 1) + ")");
            
            int totalItemCount = 0;
            if (dataPackage.getItems() != null) {
                for(ItemStack it : dataPackage.getItems()) {
                    if(it != null) totalItemCount += it.getAmount();
                }
            }
            
            meta.setLore(Arrays.asList(
                "§7จำนวน: " + totalItemCount + " ชิ้น",
                "§bXP: " + dataPackage.getExperience(),
                "§aราคา: " + currencyCost + " " + currencyName,
                "",
                "§eคลิกเพื่อซื้อคืน"
            ));
            guiItem.setItemMeta(meta);
            gui.setItem(slot, guiItem);
        }

        if (page > 0) {
            gui.setItem(45, PREV_PAGE_ITEM);
        }
        if (endIndex < totalItems) {
            gui.setItem(53, NEXT_PAGE_ITEM);
        }

        player.openInventory(gui);
        if (page == 0) {
            logger.log(LoggingService.LogLevel.INFO, player.getName() + " เปิดหน้าต่าง /buyback");
        }
    }

    public void openAdminChestGUI(Player admin, OfflinePlayer targetPlayer) {
        openAdminChestGUI(admin, targetPlayer, 0);
    }

    public void openAdminChestGUI(Player admin, OfflinePlayer targetPlayer, int page) {
        UUID targetUUID = targetPlayer.getUniqueId();
        adminViewing.put(admin.getUniqueId(), targetUUID);
        playerPages.put(admin.getUniqueId(), page);

        List<Location> chestLocations = plugin.getDeathChestManager().getActiveChestLocations(targetUUID);
        List<DeathDataPackage> buybackItems = storageManager.getLostItems(targetUUID);

        int totalActiveChests = (chestLocations != null) ? chestLocations.size() : 0; // Fix NPE
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
                // This is an Active Chest
                Location loc = chestLocations.get(i);
                DeathChestData data = plugin.getDeathChestManager().getActiveChestAt(loc);

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
                // This is a Buyback Item
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

    public void handleGuiClick(Player player, int slot, ItemStack clickedItem) {
        String title = player.getOpenInventory().getTitle();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        
        if (slot == 45 && clickedItem.isSimilar(PREV_PAGE_ITEM)) {
            int newPage = currentPage - 1;
            if (title.startsWith(GUI_TITLE_PREFIX)) {
                openBuybackGUI(player, newPage);
            } else if (title.startsWith(ADMIN_GUI_TITLE_PREFIX)) {
                UUID targetUUID = getAdminViewing(player.getUniqueId());
                if (targetUUID != null) {
                    openAdminChestGUI(player, Bukkit.getOfflinePlayer(targetUUID), newPage);
                }
            }
            return;
        }
        
        if (slot == 53 && clickedItem.isSimilar(NEXT_PAGE_ITEM)) {
            int newPage = currentPage + 1;
            if (title.startsWith(GUI_TITLE_PREFIX)) {
                openBuybackGUI(player, newPage);
            } else if (title.startsWith(ADMIN_GUI_TITLE_PREFIX)) {
                UUID targetUUID = getAdminViewing(player.getUniqueId());
                if (targetUUID != null) {
                    openAdminChestGUI(player, Bukkit.getOfflinePlayer(targetUUID), newPage);
                }
            }
            return;
        }

        if (title.startsWith(GUI_TITLE_PREFIX)) {
            handleBuybackClick(player, slot, clickedItem, currentPage);
        } else if (title.startsWith(ADMIN_GUI_TITLE_PREFIX)) {
            handleAdminGuiClick(player, slot, clickedItem, currentPage);
        }
    }

    private void handleAdminGuiClick(Player admin, int slot, ItemStack clickedItem, int currentPage) {
        if (clickedItem.getType() != Material.CHEST && clickedItem.getType() != Material.ENDER_CHEST) {
            return;
        }

        UUID targetUUID = getAdminViewing(admin.getUniqueId());
        if (targetUUID == null) {
            admin.closeInventory();
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUUID); // Get target player for logging

        int index = (currentPage * ITEMS_PER_PAGE) + slot;
        List<Location> chestLocations = plugin.getDeathChestManager().getActiveChestLocations(targetUUID);
        int totalActiveChests = (chestLocations != null) ? chestLocations.size() : 0;

        if (index < totalActiveChests) {
            // It's an active chest, teleport
            if (index >= chestLocations.size()) {
                admin.sendMessage("§cเกิดข้อผิดพลาด: ไม่พบรายการกล่อง (Index: " + index + ")");
                admin.closeInventory();
                return;
            }

            Location targetLoc = chestLocations.get(index);
            // [LOGGING FIX] Get location string from data
            DeathChestData data = plugin.getDeathChestManager().getActiveChestAt(targetLoc);
            String locationString = (data != null) ? data.locationString : "N/A";

            Location safeLoc = targetLoc.clone().add(0.5, 1.0, 0.5);
            
            admin.teleport(safeLoc);
            admin.sendMessage(configManager.getChatMessageAdminTeleported().replace("&", "§"));
            
            // [LOGGING FIX] Log TP success
            logger.logAdminTpSuccess(admin, targetPlayer, locationString);
            
            admin.closeInventory();
        } else {
            // It's a buyback item, send message
            int buybackIndex = index - totalActiveChests;
            
            // [LOGGING FIX] Log TP fail
            logger.logAdminTpFailBuyback(admin, targetPlayer, buybackIndex);

            admin.sendMessage(configManager.getChatMessageAdminIsBuyback().replace("&", "§"));
            // Don't close inventory
        }
    }

    private void handleBuybackClick(Player player, int slot, ItemStack clickedItem, int currentPage) {
        if (clickedItem.getType() != Material.CHEST) {
            return;
        }

        int index = (currentPage * ITEMS_PER_PAGE) + slot;
        List<DeathDataPackage> playerLostItems = storageManager.getLostItems(player.getUniqueId());

        if (playerLostItems == null || index >= playerLostItems.size()) {
            player.sendMessage("§cเกิดข้อผิดพลาด: ไม่พบรายการของ (Index: " + index + ")");
            player.closeInventory();
            return;
        }

        int cost = configManager.getBuybackCost();
        double balance = hookManager.getBalance(player);
        
        if (balance >= cost) {
            DeathDataPackage dataToGive = storageManager.removeLostItems(player.getUniqueId(), index);
            
            if (dataToGive == null) {
                 player.sendMessage("§cเกิดข้อผิดพลาด: ไม่พบรายการของ (อาจถูกซื้อไปแล้ว)");
                 logger.log(LoggingService.LogLevel.WARN, player.getName() + " คลิกซื้อของใน GUI แต่ไม่พบ item (Index " + index + ")");
                 player.closeInventory();
                 return;
            }
            
            hookManager.withdrawMoney(player, cost);
            
            if (dataToGive.getItems() != null) {
                for (ItemStack item : dataToGive.getItems()) {
                    if(item != null) {
                        player.getInventory().addItem(item);
                    }
                }
            }
            player.giveExp(dataToGive.getExperience()); 
            
            player.sendMessage("§aซื้อของคืนสำเร็จ! (ได้รับ XP: " + dataToGive.getExperience() + ")");
            logger.logBuyback(player, index + 1, cost, hookManager.getActiveCurrencyName(), dataToGive.getExperience());
            
            player.closeInventory();
            
        } else {
            player.sendMessage("§cคุณมีเงินไม่พอ! (" + hookManager.getActiveCurrencyName() + " ไม่พอ)");
            logger.log(LoggingService.LogLevel.WARN, player.getName() + " พยายามซื้อของคืนแต่เงินไม่พอ (มี " + balance + " ต้องการ " + cost + ")");
            player.closeInventory();
        }
    }
}