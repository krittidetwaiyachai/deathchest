package xyz.kaijiieow.deathchest;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List; // [NEW]
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TeleportChestCommand implements CommandExecutor {

    private final DeathChestManager deathChestManager;
    private final ConfigManager configManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TeleportChestCommand(DeathChestManager deathChestManager, ConfigManager configManager) {
        this.deathChestManager = deathChestManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("ต้องเป็นผู้เล่นเท่านั้น");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("deathchest.tp")) {
            player.sendMessage(configManager.getChatMessageNoPermission().replace("&", "§"));
            return true;
        }

        if (!player.hasPermission("deathchest.tp.bypass")) {
            long cooldownSeconds = configManager.getTpChestCooldown();
            if (cooldowns.containsKey(player.getUniqueId())) {
                long lastUsed = cooldowns.get(player.getUniqueId());
                long timeElapsed = System.currentTimeMillis() - lastUsed;
                long timeRemaining = TimeUnit.SECONDS.toMillis(cooldownSeconds) - timeElapsed;

                if (timeRemaining > 0) {
                    long secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) + 1; 
                    player.sendMessage(
                        configManager.getChatMessageTeleportCooldown()
                            .replace("&", "§")
                            .replace("%time%", String.valueOf(secondsRemaining))
                    );
                    return true;
                }
            }
        }

        // [EDIT] Get list of chests
        List<Location> chestLocs = deathChestManager.getActiveChestLocations(player.getUniqueId());

        if (chestLocs == null || chestLocs.isEmpty()) {
            player.sendMessage(configManager.getChatMessageNoChestFound().replace("&", "§"));
            return true;
        }
        
        // [EDIT] Get the most recent chest
        Location chestLoc = chestLocs.get(chestLocs.size() - 1); 

        Location safeLoc = chestLoc.clone().add(0.5, 1.0, 0.5);
        player.teleport(safeLoc);
        player.sendMessage(configManager.getChatMessageTeleported().replace("&", "§"));
        
        if (!player.hasPermission("deathchest.tp.bypass")) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis()); 
        }
        
        return true;
    }
}