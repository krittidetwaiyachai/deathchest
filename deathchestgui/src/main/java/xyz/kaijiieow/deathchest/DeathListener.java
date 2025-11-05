package xyz.kaijiieow.deathchest;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    private final DeathChestManager deathChestManager;

    public DeathListener(DeathChestManager deathChestManager) {
        this.deathChestManager = deathChestManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        deathChestManager.createDeathChest(event);
    }
}