package xyz.kaijiieow.deathchest;

public class DatabaseChestData {
    public final String ownerUuid, world, itemsBase64;
    public final int x, y, z, experience;
    public final long createdAt; // [FIX]

    public DatabaseChestData(String ownerUuid, String world, int x, int y, int z, String itemsBase64, int experience, long createdAt) { // [FIX]
        this.ownerUuid = ownerUuid;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.itemsBase64 = itemsBase64;
        this.experience = experience;
        this.createdAt = createdAt; // [FIX]
    }
}