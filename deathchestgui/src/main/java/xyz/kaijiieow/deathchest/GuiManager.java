package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class GuiManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final HookManager hookManager;
    private final StorageManager storageManager;
    private final LoggingService logger;

    public static final String GUI_TITLE = "§4[Buyback] - ซื้อของคืน";

    public GuiManager(DeathChestPlugin plugin, ConfigManager configManager, HookManager hookManager, StorageManager storageManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.hookManager = hookManager;
        this.storageManager = storageManager;
        this.logger = logger;
    }

    public void openBuybackGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        List<ItemStack[]> playerLostItems = storageManager.getLostItems(player.getUniqueId());

        if (playerLostItems == null || playerLostItems.isEmpty()) {
            player.sendMessage("§eคุณไม่มีรายการของที่สามารถซื้อคืนได้");
            return;
        }

        int slot = 0;
        String currencyName = configManager.getCurrencyName();
        int cost = configManager.getBuybackCost();
        
        for (ItemStack[] itemSet : playerLostItems) {
            if (slot >= 54) break;

            ItemStack guiItem = new ItemStack(Material.CHEST);
            ItemMeta meta = guiItem.getItemMeta();
            meta.setDisplayName("§cของที่หายไป (Set " + (slot + 1) + ")");
            
            int totalItems = 0;
            for(ItemStack it : itemSet) if(it != null) totalItems += it.getAmount();
            
            meta.setLore(Arrays.asList(
                "§7จำนวน: " + totalItems + " ชิ้น",
                "§aราคา: " + cost + " " + currencyName,
                "",
                "§eคลิกเพื่อซื้อคืน"
            ));
            guiItem.setItemMeta(meta);
            gui.setItem(slot, guiItem);
            slot++;
        }

        player.openInventory(gui);
        logger.log(LoggingService.LogLevel.INFO, player.getName() + " เปิดหน้าต่าง /buyback");
    }

    public void handleGuiClick(Player player, int slot) {
        List<ItemStack[]> playerLostItems = storageManager.getLostItems(player.getUniqueId());

        if (playerLostItems == null || slot >= playerLostItems.size()) {
            player.sendMessage("§cเกิดข้อผิดพลาด: ไม่พบรายการของ");
            player.closeInventory();
            return;
        }

        int cost = configManager.getBuybackCost();
        double balance = hookManager.getBalance(player);
        
        if (balance >= cost) {
            ItemStack[] itemsToGive = storageManager.removeLostItems(player.getUniqueId(), slot);
            
            if (itemsToGive == null) {
                 player.sendMessage("§cเกิดข้อผิดพลาด: ไม่พบรายการของ (อาจถูกซื้อไปแล้ว)");
                 logger.log(LoggingService.LogLevel.WARN, player.getName() + " คลิกซื้อของใน GUI แต่ไม่พบ item (slot " + slot + ")");
                 player.closeInventory();
                 return;
            }
            
            hookManager.withdrawMoney(player, cost);
            
            for (ItemStack item : itemsToGive) {
                if(item != null) {
                    player.getInventory().addItem(item);
                }
            }
            
            player.sendMessage("§aซื้อของคืนสำเร็จ!");
            logger.log(LoggingService.LogLevel.INFO, player.getName() + " ซื้อของคืน (Set " + (slot + 1) + ") ราคา " + cost + " " + configManager.getCurrencyName());
            player.closeInventory();
            
        } else {
            player.sendMessage("§cคุณมีเงินไม่พอ! (" + configManager.getCurrencyName() + " ไม่พอ)");
            logger.log(LoggingService.LogLevel.WARN, player.getName() + " พยายามซื้อของคืนแต่เงินไม่พอ (มี " + balance + " ต้องการ " + cost + ")");
            player.closeInventory();
        }
    }
}