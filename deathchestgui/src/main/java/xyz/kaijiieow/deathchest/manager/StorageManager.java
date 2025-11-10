package xyz.kaijiieow.deathchest.manager;

import org.bukkit.inventory.ItemStack;
import xyz.kaijiieow.deathchest.database.BuybackDatabase;
import xyz.kaijiieow.deathchest.database.DatabaseManager;
import xyz.kaijiieow.deathchest.model.DatabaseBuybackData;
import xyz.kaijiieow.deathchest.model.DeathDataPackage;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.util.SerializationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StorageManager {

    private final Map<UUID, List<DeathDataPackage>> lostItemsStorage = new HashMap<>();
    private final BuybackDatabase buybackDatabase;
    private final LoggingService logger;

    public StorageManager(DatabaseManager databaseManager, LoggingService logger) {
        this.buybackDatabase = databaseManager.getBuybackDatabase();
        this.logger = logger;
    }

    public void loadBuybackItemsFromDatabase() {
        List<DatabaseBuybackData> dbItems = buybackDatabase.loadAllBuybackItems();
        if (dbItems.isEmpty()) {
            return;
        }

        int count = 0;
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

    public void addBuybackItem(UUID playerId, ItemStack[] items, int experience) {
        long dbId = buybackDatabase.saveBuybackItem(playerId, items, experience);
        if (dbId == -1) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถเซฟ Buyback item ลง DB ได้!");
            return;
        }

        DeathDataPackage dataPackage = new DeathDataPackage(dbId, items, experience);
        lostItemsStorage.putIfAbsent(playerId, new ArrayList<>());
        lostItemsStorage.get(playerId).add(dataPackage);
    }

    public DeathDataPackage removeLostItems(UUID playerId, int index) {
        List<DeathDataPackage> playerItems = getLostItems(playerId);
        if (playerItems == null || index < 0 || index >= playerItems.size()) {
            return null;
        }
        
        DeathDataPackage pkg = playerItems.remove(index);
        buybackDatabase.deleteBuybackItem(pkg.getDatabaseId());
        
        return pkg;
    }

    public boolean hasLostItems(UUID playerId) {
        List<DeathDataPackage> playerItems = getLostItems(playerId);
        return playerItems != null && !playerItems.isEmpty();
    }
}

