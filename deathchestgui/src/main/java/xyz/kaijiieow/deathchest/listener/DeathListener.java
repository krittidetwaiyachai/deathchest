package xyz.kaijiieow.deathchest.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.kaijiieow.deathchest.manager.DeathChestManager;

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

