package xyz.kaijiieow.deathchest.model;

public class DatabaseBuybackData {
    public final long id;
    public final String ownerUuid;
    public final String itemsBase64;
    public final int experience;

    public DatabaseBuybackData(long id, String ownerUuid, String itemsBase64, int experience) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.itemsBase64 = itemsBase64;
        this.experience = experience;
    }
}

