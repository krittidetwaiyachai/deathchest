package xyz.kaijiieow.deathchest;

// [FIX] ไฟล์นี้ต้องไม่มี import ของ bukkit
// มันเป็นแค่ DTO (Data Transfer Object)
public class DatabaseChestData {
    String ownerUuid;
    String ownerName; // [FIX 4.2] เพิ่ม field นี้
    String world;
    int x, y, z;
    String itemsBase64;
    int experience;
    int remainingSeconds;
    long createdAt; // [FIX] เพิ่มเวลาที่สร้าง (ตามที่มึงขอ)

    // [FIX 4.2] นี่คือ constructor ที่ DatabaseManager จะเรียก
    public DatabaseChestData(String ownerUuid, String ownerName, String world, int x, int y, int z, String itemsBase64, int experience, int remainingSeconds, long createdAt) {
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName; // [FIX 4.2]
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