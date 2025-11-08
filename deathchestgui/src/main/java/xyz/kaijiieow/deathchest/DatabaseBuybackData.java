package xyz.kaijiieow.deathchest;

// [NEW FILE] ย้ายคลาสออกมาจาก DatabaseManager เพื่อแก้ "cannot be resolved"
public class DatabaseBuybackData {
    public final long id;
    public final String ownerUuid, itemsBase64;
    public final int experience;

    public DatabaseBuybackData(long id, String ownerUuid, String itemsBase64, int experience) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.itemsBase64 = itemsBase64;
        this.experience = experience;
    }
}