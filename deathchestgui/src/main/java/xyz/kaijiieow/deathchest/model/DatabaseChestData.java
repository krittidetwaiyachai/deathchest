package xyz.kaijiieow.deathchest.model;

public class DatabaseChestData {
    public String ownerUuid;
    public String ownerName;
    public String world;
    public int x, y, z;
    public String itemsBase64;
    public int experience;
    public int remainingSeconds;
    public long createdAt;

    public DatabaseChestData(String ownerUuid, String ownerName, String world, int x, int y, int z, String itemsBase64, int experience, int remainingSeconds, long createdAt) {
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.itemsBase64 = itemsBase64;
        this.experience = experience;
        this.remainingSeconds = remainingSeconds;
        this.createdAt = createdAt;
    }
}

