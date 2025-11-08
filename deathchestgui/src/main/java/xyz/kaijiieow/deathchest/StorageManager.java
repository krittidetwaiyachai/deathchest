package xyz.kaijiieow.deathchest;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// [FIX] เพิ่ม import ของคลาสใหม่
import xyz.kaijiieow.deathchest.DatabaseBuybackData;

public class StorageManager {

    private final Map<UUID, List<DeathDataPackage>> lostItemsStorage = new HashMap<>();
    private DatabaseManager databaseManager; // [NEW]
    private LoggingService logger; // [NEW]

    // [MODIFIED] Constructor
    public StorageManager(DatabaseManager databaseManager, LoggingService logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    // [NEW] Load all data from DB on startup
    public void loadBuybackItemsFromDatabase() {
        // [FIX] แก้ Type จาก DatabaseManager.DatabaseBuybackData เป็น DatabaseBuybackData
        List<DatabaseBuybackData> dbItems = databaseManager.loadAllBuybackItems();
        if (dbItems.isEmpty()) {
            return;
        }

        int count = 0;
        // [FIX] แก้ Type จาก DatabaseManager.DatabaseBuybackData เป็น DatabaseBuybackData
        for (DatabaseBuybackData dbData : dbItems) {
            try {
                UUID ownerUuid = UUID.fromString(dbData.ownerUuid);
                ItemStack[] items = SerializationUtils.itemStackArrayFromBase64(dbData.itemsBase64);
                DeathDataPackage pkg = new DeathDataPackage(dbData.id, items, dbData.experience);

                lostItemsStorage.putIfAbsent(ownerUuid, new ArrayList<>());
                lostItemsStorage.get(ownerUuid).add(pkg);
                count++;
            } catch (Exception e) {
                logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถโหลด Buyback item ID: " + dbData.id + " - " + e.getMessage());
            }
        }
        logger.log(LoggingService.LogLevel.INFO, "โหลด Buyback items " + count + " รายการ จาก Database");
    }

    public List<DeathDataPackage> getLostItems(UUID playerId) {
        return lostItemsStorage.get(playerId);
    }

    // [MODIFIED] This method now saves to DB and cache
    public void addBuybackItem(UUID playerId, ItemStack[] items, int experience) {
        long dbId = databaseManager.saveBuybackItem(playerId, items, experience);
        if (dbId == -1) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถเซฟ Buyback item ลง DB ได้!");
            return;
        }

        DeathDataPackage dataPackage = new DeathDataPackage(dbId, items, experience);
        lostItemsStorage.putIfAbsent(playerId, new ArrayList<>());
        lostItemsStorage.get(playerId).add(dataPackage);
    }

    // [MODIFIED] This method now removes from DB and cache
    public DeathDataPackage removeLostItems(UUID playerId, int index) {
        List<DeathDataPackage> playerItems = getLostItems(playerId);
        if (playerItems == null || index < 0 || index >= playerItems.size()) {
            return null;
        }
        
        DeathDataPackage pkg = playerItems.remove(index); // Remove from cache
        databaseManager.deleteBuybackItem(pkg.getDatabaseId()); // Remove from DB
        
        return pkg;
    }

    public boolean hasLostItems(UUID playerId) {
        List<DeathDataPackage> playerItems = getLostItems(playerId);
        return playerItems != null && !playerItems.isEmpty();
    }
}