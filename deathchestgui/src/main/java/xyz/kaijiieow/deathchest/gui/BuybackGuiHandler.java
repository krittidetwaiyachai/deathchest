package xyz.kaijiieow.deathchest.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.model.DeathDataPackage;
import xyz.kaijiieow.deathchest.manager.HookManager;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.manager.StorageManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BuybackGuiHandler {
    
    private final ConfigManager configManager;
    private final HookManager hookManager;
    private final StorageManager storageManager;
    private final LoggingService logger;
    
    public static final String GUI_TITLE_PREFIX = "§4[Buyback] - ซื้อของคืน";
    private static final int ITEMS_PER_PAGE = 45;
    
    private final ItemStack NEXT_PAGE_ITEM;
    private final ItemStack PREV_PAGE_ITEM;
    
    public BuybackGuiHandler(ConfigManager configManager, HookManager hookManager, StorageManager storageManager, LoggingService logger) {
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
    
    public void openBuybackGUI(Player player, int page, Map<UUID, Integer> playerPages) {
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
    
    public void handleBuybackClick(Player player, int slot, ItemStack clickedItem, int currentPage, Map<UUID, Integer> playerPages) {
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
    
    public ItemStack getNextPageItem() {
        return NEXT_PAGE_ITEM;
    }
    
    public ItemStack getPrevPageItem() {
        return PREV_PAGE_ITEM;
    }
}

