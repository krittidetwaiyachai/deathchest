package xyz.kaijiieow.deathchest;

// [FIX] ไฟล์นี้ต้องไม่มี import ของ bukkit
// มันเป็นแค่ DTO (Data Transfer Object)
public class DatabaseChestData {
    String ownerUuid;
    String world;
    int x, y, z;
    String itemsBase64;
    int experience;
    int remainingSeconds;
    long createdAt; // [FIX] เพิ่มเวลาที่สร้าง (ตามที่มึงขอ)

    // [FIX] นี่คือ constructor ที่ DatabaseManager จะเรียก
    public DatabaseChestData(String ownerUuid, String world, int x, int y, int z, String itemsBase64, int experience, int remainingSeconds, long createdAt) {
        this.ownerUuid = ownerUuid;
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