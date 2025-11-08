package xyz.kaijiieow.deathchest;

// [NEW FILE] ย้ายคลาสออกมาจาก DatabaseManager เพื่อแก้ "cannot be resolved"
public class DatabaseChestData {
    public final String ownerUuid, world, itemsBase64;
    public final int x, y, z, experience;

    public DatabaseChestData(String ownerUuid, String world, int x, int y, int z, String itemsBase64, int experience) {
        this.ownerUuid = ownerUuid;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.itemsBase64 = itemsBase64;
        this.experience = experience;
    }
}