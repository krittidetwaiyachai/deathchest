package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
    private final Map<UUID, Integer> playerPages = new HashMap<>();
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
        String currencyName = configManager.getCurrencyName();

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            DeathDataPackage dataPackage = playerLostItems.get(i);

            ItemStack guiItem = new ItemStack(Material.CHEST);
            ItemMeta meta = guiItem.getItemMeta();
            meta.setDisplayName("§cของที่หายไป (Set " + (i + 1) + ")");
            
            int totalItemCount = 0;
            for(ItemStack it : dataPackage.getItems()) {
                if(it != null) totalItemCount += it.getAmount();
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

    public void handleGuiClick(Player player, int slot, ItemStack clickedItem) {
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        
        if (slot == 45 && clickedItem.isSimilar(PREV_PAGE_ITEM)) {
            openBuybackGUI(player, currentPage - 1);
            return;
        }
        
        if (slot == 53 && clickedItem.isSimilar(NEXT_PAGE_ITEM)) {
            openBuybackGUI(player, currentPage + 1);
            return;
        }

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
            
            for (ItemStack item : dataToGive.getItems()) {
                if(item != null) {
                    player.getInventory().addItem(item);
                }
            }
            player.giveExp(dataToGive.getExperience()); 
            
            player.sendMessage("§aซื้อของคืนสำเร็จ! (ได้รับ XP: " + dataToGive.getExperience() + ")");
            logger.logBuyback(player, index + 1, cost, configManager.getCurrencyName(), dataToGive.getExperience());
            
            player.closeInventory();
            
        } else {
            player.sendMessage("§cคุณมีเงินไม่พอ! (" + configManager.getCurrencyName() + " ไม่พอ)");
            logger.log(LoggingService.LogLevel.WARN, player.getName() + " พยายามซื้อของคืนแต่เงินไม่พอ (มี " + balance + " ต้องการ " + cost + ")");
            player.closeInventory();
        }
    }
}