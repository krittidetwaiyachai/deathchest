package xyz.kaijiieow.deathchest;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StorageManager {

    private final Map<UUID, List<DeathDataPackage>> lostItemsStorage = new HashMap<>();

    public List<DeathDataPackage> getLostItems(UUID playerId) {
        return lostItemsStorage.get(playerId);
    }

    public void addLostItems(UUID playerId, DeathDataPackage dataPackage) {
        lostItemsStorage.putIfAbsent(playerId, new ArrayList<>());
        lostItemsStorage.get(playerId).add(dataPackage);
    }

    public DeathDataPackage removeLostItems(UUID playerId, int index) {
        List<DeathDataPackage> playerItems = getLostItems(playerId);
        if (playerItems == null || index < 0 || index >= playerItems.size()) {
            return null;
        }
        return playerItems.remove(index);
    }

    public boolean hasLostItems(UUID playerId) {
        List<DeathDataPackage> playerItems = getLostItems(playerId);
        return playerItems != null && !playerItems.isEmpty();
    }
}