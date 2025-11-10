package xyz.kaijiieow.deathchest.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.kaijiieow.deathchest.gui.GuiManager;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.manager.DeathChestManager;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.util.LoggingService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminChestCommand implements CommandExecutor, TabCompleter {

    private final GuiManager guiManager;
    private final ConfigManager configManager;
    private final LoggingService logger;
    private final DeathChestManager deathChestManager;

    public AdminChestCommand(GuiManager guiManager, ConfigManager configManager, LoggingService logger, DeathChestManager deathChestManager) {
        this.guiManager = guiManager;
        this.configManager = configManager;
        this.logger = logger;
        this.deathChestManager = deathChestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("คำสั่งนี้ใช้ได้เฉพาะผู้เล่นเท่านั้น");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("deathchest.admin")) {
            player.sendMessage(configManager.getChatMessageNoPermission().replace("&", "§"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§cUsage: /dctp <player> หรือ /dctp tp <player>");
            return true;
        }

        String targetName = args[0];
        if(args.length > 1) {
            targetName = args[1];
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            player.sendMessage(configManager.getChatMessageAdminNoPlayerFound().replace("&", "§").replace("%player%", targetName));
            return true;
        }
        
        UUID targetUUID = targetPlayer.getUniqueId();

        if (args.length > 0 && args[0].equalsIgnoreCase("tp")) {
            List<BlockLocation> chestLocations = deathChestManager.getActiveChestLocations(targetUUID);
            if (chestLocations.isEmpty()) {
                player.sendMessage(configManager.getChatMessageAdminNoChests().replace("&", "§").replace("%player%", targetPlayer.getName()));
                return true;
            }

            BlockLocation targetKey = chestLocations.get(0);
            DeathChestData data = deathChestManager.getActiveChestAt(targetKey);
            
            World world = Bukkit.getWorld(targetKey.worldName());
            if (world == null) {
                player.sendMessage("§cError: ไม่สามารถหาโลกของกล่องศพได้ (โลกอาจจะยังไม่ได้โหลด)");
                return true;
            }

            Location safeLoc = new Location(world, targetKey.x() + 0.5, targetKey.y() + 1.0, targetKey.z() + 0.5);

            player.teleport(safeLoc);
            player.sendMessage(configManager.getChatMessageAdminTeleported().replace("&", "§"));
            
            String locationString = (data != null) ? data.locationString : "N/A";
            logger.logAdminTpSuccess(player, targetPlayer, locationString);
            
        } else {
            guiManager.openAdminChestGUI(player, targetPlayer);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("tp");
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

