package xyz.kaijiieow.deathchest.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.kaijiieow.deathchest.gui.AdminGuiHandler;
import xyz.kaijiieow.deathchest.gui.BuybackGuiHandler;
import xyz.kaijiieow.deathchest.gui.GuiManager;

public class GuiListener implements Listener {

    private final GuiManager guiManager;

    public GuiListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        if (!title.startsWith(BuybackGuiHandler.GUI_TITLE_PREFIX) && !title.startsWith(AdminGuiHandler.ADMIN_GUI_TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        guiManager.handleGuiClick(player, slot, clickedItem);
    }
}

