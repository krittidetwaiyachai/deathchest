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
import xyz.kaijiieow.deathchest.manager.HookManager;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeleportChestCommand implements CommandExecutor {

    private final DeathChestManager deathChestManager;
    private final ConfigManager configManager;
    private final HookManager hookManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TeleportChestCommand(DeathChestPlugin plugin, DeathChestManager deathChestManager, ConfigManager configManager, HookManager hookManager) {
        this.deathChestManager = deathChestManager;
        this.configManager = configManager;
        this.hookManager = hookManager;
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

        // ตรวจสอบค่าใช้จ่ายตามยศ
        String playerGroup = hookManager.getPlayerGroup(player);
        double cost = configManager.getTpChestCostForGroup(playerGroup);
        
        if (cost > 0) {
            double balance = hookManager.getBalance(player);
            if (balance < cost) {
                String currencyName = hookManager.getActiveCurrencyName();
                String message = configManager.getChatMessageTpchestInsufficientFunds()
                    .replace("&", "§")
                    .replace("%cost%", String.valueOf(cost))
                    .replace("%currency%", currencyName)
                    .replace("%balance%", String.valueOf(balance));
                player.sendMessage(message);
                return true;
            }
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

        // หักเงินก่อน teleport
        if (cost > 0) {
            hookManager.withdrawMoney(player, cost);
            String currencyName = hookManager.getActiveCurrencyName();
            String message = configManager.getChatMessageTpchestCharged()
                .replace("&", "§")
                .replace("%cost%", String.valueOf(cost))
                .replace("%currency%", currencyName);
            player.sendMessage(message);
        }

        player.teleport(tpLoc);
        player.sendMessage(configManager.getChatMessageTeleported().replace("&", "§"));
        cooldowns.put(player.getUniqueId(), currentTime);

        return true;
    }
}

