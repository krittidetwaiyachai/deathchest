package xyz.kaijiieow.deathchest;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// จัดการคลังของที่รอซื้อคืน
public class StorageManager {

    // !! สำคัญ: ยังเป็น HashMap ชั่วคราว (ของหายเมื่อรีเซิร์ฟ) !!
    // !! อนาคตควรเปลี่ยนส่วนนี้เป็น SQL !!
    private final Map<UUID, List<ItemStack[]>> lostItemsStorage = new HashMap<>();

    public List<ItemStack[]> getLostItems(UUID playerId) {
        return lostItemsStorage.get(playerId);
    }

    public void addLostItems(UUID playerId, ItemStack[] items) {
        lostItemsStorage.putIfAbsent(playerId, new ArrayList<>());
        lostItemsStorage.get(playerId).add(items);
    }

    public ItemStack[] removeLostItems(UUID playerId, int index) {
        List<ItemStack[]> playerItems = getLostItems(playerId);
        if (playerItems == null || index < 0 || index >= playerItems.size()) {
            return null;
        }
        return playerItems.remove(index);
    }

    public boolean hasLostItems(UUID playerId) {
        List<ItemStack[]> playerItems = getLostItems(playerId);
        return playerItems != null && !playerItems.isEmpty();
    }
}