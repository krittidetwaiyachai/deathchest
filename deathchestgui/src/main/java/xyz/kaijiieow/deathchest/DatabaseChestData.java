package xyz.kaijiieow.deathchest;

public class DatabaseChestData {
    String ownerUuid;
    String world;
    int x, y, z;
    String itemsBase64;
    int experience;
    int remainingSeconds; // <-- [FIX] เปลี่ยนจาก long createdAt

    public DatabaseChestData(String ownerUuid, String world, int x, int y, int z, String itemsBase64, int experience, int remainingSeconds) { // <-- [FIX] เปลี่ยน
        this.ownerUuid = ownerUuid;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.itemsBase64 = itemsBase64;
        this.experience = experience;
        this.remainingSeconds = remainingSeconds; // <-- [FIX] เปลี่ยน
    }
}