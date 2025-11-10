package xyz.kaijiieow.deathchest.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.manager.DeathChestManager;
import xyz.kaijiieow.deathchest.model.BlockLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            sender.sendMessage("คำสั่งนี้ใช้ได้เฉพาะผู้เล่นเท่านั้น");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("deathchest.tpchest")) {
            player.sendMessage(configManager.getChatMessageNoPermission().replace("&", "§"));
            return true;
        }

        List<BlockLocation> chestLocations = deathChestManager.getActiveChestLocations(player.getUniqueId());
        if (chestLocations.isEmpty()) {
            player.sendMessage(configManager.getChatMessageNoChestFound().replace("&", "§"));
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownTime = configManager.getTpChestCooldown() * 1000L;
        if (cooldowns.containsKey(player.getUniqueId())) {
            long lastUsed = cooldowns.get(player.getUniqueId());
            long remaining = (lastUsed + cooldownTime) - currentTime;
            if (remaining > 0) {
                player.sendMessage(configManager.getChatMessageTeleportCooldown()
                        .replace("&", "§")
                        .replace("%time%", String.valueOf(remaining / 1000 + 1))
                );
                return true;
            }
        }

        BlockLocation key = chestLocations.get(0);
        World world = Bukkit.getWorld(key.worldName());

        if (world == null) {
            player.sendMessage("§cError: ไม่สามารถหาโลกของกล่องศพได้ (โลกอาจจะยังไม่ได้โหลด)");
            return true;
        }

        Location tpLoc = new Location(world, key.x() + 0.5, key.y() + 1.0, key.z() + 0.5);

        player.teleport(tpLoc);
        player.sendMessage(configManager.getChatMessageTeleported().replace("&", "§"));
        cooldowns.put(player.getUniqueId(), currentTime);

        return true;
    }
}

