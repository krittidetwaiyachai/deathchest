package xyz.kaijiieow.deathchest;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap; // [NEW]
import java.util.Map;     // [NEW]
import java.util.UUID;    // [NEW]
import java.util.concurrent.TimeUnit; // [NEW]

public class TeleportChestCommand implements CommandExecutor {

    private final DeathChestManager deathChestManager;
    private final ConfigManager configManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // [NEW] เก็บเวลาที่ใช้คำสั่งล่าสุด

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

        // --- [NEW] Cooldown Check ---
        // (ข้ามเช็คคูลดาวน์ ถ้ามียศ deathchest.tp.bypass)
        if (!player.hasPermission("deathchest.tp.bypass")) {
            long cooldownSeconds = configManager.getTpChestCooldown();
            if (cooldowns.containsKey(player.getUniqueId())) {
                long lastUsed = cooldowns.get(player.getUniqueId());
                long timeElapsed = System.currentTimeMillis() - lastUsed;
                long timeRemaining = TimeUnit.SECONDS.toMillis(cooldownSeconds) - timeElapsed;

                if (timeRemaining > 0) {
                    long secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) + 1; // ปัดเศษขึ้น
                    player.sendMessage(
                        configManager.getChatMessageTeleportCooldown()
                            .replace("&", "§")
                            .replace("%time%", String.valueOf(secondsRemaining))
                    );
                    return true;
                }
            }
        }
        // --- End Cooldown Check ---

        Location chestLoc = deathChestManager.getPlayerChestLocation(player.getUniqueId());

        if (chestLoc == null) {
            player.sendMessage(configManager.getChatMessageNoChestFound().replace("&", "§"));
            return true;
        }

        Location safeLoc = chestLoc.clone().add(0.5, 1.0, 0.5);
        player.teleport(safeLoc);
        player.sendMessage(configManager.getChatMessageTeleported().replace("&", "§"));
        
        // [NEW] ตั้งค่าคูลดาวน์ (เฉพาะถ้ามึงไม่มียศ bypass)
        if (!player.hasPermission("deathchest.tp.bypass")) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis()); 
        }
        
        return true;
    }
}