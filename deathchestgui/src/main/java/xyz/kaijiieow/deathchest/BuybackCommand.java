package xyz.kaijiieow.deathchest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// จัดการคำสั่ง /buyback
public class BuybackCommand implements CommandExecutor {

    private final GuiManager guiManager;

    public BuybackCommand(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("ต้องเป็นผู้เล่นเท่านั้น");
            return true;
        }

        Player player = (Player) sender;
        guiManager.openBuybackGUI(player);
        return true;
    }
}