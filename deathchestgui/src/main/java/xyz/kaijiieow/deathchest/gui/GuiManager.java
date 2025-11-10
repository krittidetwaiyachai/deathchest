package xyz.kaijiieow.deathchest.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.manager.HookManager;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.manager.StorageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    private final BuybackGuiHandler buybackGuiHandler;
    private final AdminGuiHandler adminGuiHandler;
    
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, UUID> adminViewing = new HashMap<>();

    public GuiManager(DeathChestPlugin plugin, ConfigManager configManager, HookManager hookManager, StorageManager storageManager, LoggingService logger) {
        this.buybackGuiHandler = new BuybackGuiHandler(configManager, hookManager, storageManager, logger);
        this.adminGuiHandler = new AdminGuiHandler(plugin, configManager, storageManager, logger);
    }

    public UUID getAdminViewing(UUID adminId) {
        return adminViewing.get(adminId);
    }

    public void openBuybackGUI(Player player) {
        openBuybackGUI(player, 0);
    }

    public void openBuybackGUI(Player player, int page) {
        buybackGuiHandler.openBuybackGUI(player, page, playerPages);
    }

    public void openAdminChestGUI(Player admin, OfflinePlayer targetPlayer) {
        openAdminChestGUI(admin, targetPlayer, 0);
    }

    public void openAdminChestGUI(Player admin, OfflinePlayer targetPlayer, int page) {
        adminGuiHandler.openAdminChestGUI(admin, targetPlayer, page, playerPages, adminViewing);
    }

    public void handleGuiClick(Player player, int slot, ItemStack clickedItem) {
        String title = player.getOpenInventory().getTitle();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        
        if (slot == 45 && clickedItem.isSimilar(adminGuiHandler.getPrevPageItem())) {
            int newPage = currentPage - 1;
            if (title.startsWith(BuybackGuiHandler.GUI_TITLE_PREFIX)) {
                openBuybackGUI(player, newPage);
            } else if (title.startsWith(AdminGuiHandler.ADMIN_GUI_TITLE_PREFIX)) {
                UUID targetUUID = getAdminViewing(player.getUniqueId());
                if (targetUUID != null) {
                    openAdminChestGUI(player, Bukkit.getOfflinePlayer(targetUUID), newPage);
                }
            }
            return;
        }
        
        if (slot == 53 && clickedItem.isSimilar(adminGuiHandler.getNextPageItem())) {
            int newPage = currentPage + 1;
            if (title.startsWith(BuybackGuiHandler.GUI_TITLE_PREFIX)) {
                openBuybackGUI(player, newPage);
            } else if (title.startsWith(AdminGuiHandler.ADMIN_GUI_TITLE_PREFIX)) {
                UUID targetUUID = getAdminViewing(player.getUniqueId());
                if (targetUUID != null) {
                    openAdminChestGUI(player, Bukkit.getOfflinePlayer(targetUUID), newPage);
                }
            }
            return;
        }

        if (title.startsWith(BuybackGuiHandler.GUI_TITLE_PREFIX)) {
            buybackGuiHandler.handleBuybackClick(player, slot, clickedItem, currentPage, playerPages);
        } else if (title.startsWith(AdminGuiHandler.ADMIN_GUI_TITLE_PREFIX)) {
            adminGuiHandler.handleAdminGuiClick(player, slot, clickedItem, currentPage, adminViewing);
        }
    }
}

