package xyz.kaijiieow.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminChestCommand implements CommandExecutor {

    private final GuiManager guiManager;
    private final ConfigManager configManager;
    private final LoggingService logger; // [NEW]

    public AdminChestCommand(GuiManager guiManager, ConfigManager configManager, LoggingService logger) { // [EDIT]
        this.guiManager = guiManager;
        this.configManager = configManager;
        this.logger = logger; // [NEW]
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("ต้องเป็นผู้เล่นเท่านั้น");
            return true;
        }

        Player admin = (Player) sender;

        if (!admin.hasPermission("deathchest.admin.tp")) {
            admin.sendMessage(configManager.getChatMessageNoPermission().replace("&", "§"));
            return true;
        }

        if (args.length != 1) {
            admin.sendMessage("§cUsage: /dctp <player>");
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation") // Bukkit.getOfflinePlayer(String) is deprecated
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            admin.sendMessage(configManager.getChatMessageAdminNoPlayerFound()
                .replace("&", "§")
                .replace("%player%", targetName));
            return true;
        }
        
        logger.logAdminGuiOpen(admin, targetPlayer); // [NEW] Log a GUI open
        guiManager.openAdminChestGUI(admin, targetPlayer);
        return true;
    }
}