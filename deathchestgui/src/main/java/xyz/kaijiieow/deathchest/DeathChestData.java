package xyz.kaijiieow.deathchest;

import org.bukkit.block.Chest;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DeathChestData {
    UUID ownerUUID;
    String ownerName;
    Chest chest;
    TextDisplay hologramEntity;
    ItemStack[] items;
    int experience;
    String locationString;
    int timeLeft;
    String worldName;
    int x, y, z; // <-- [FIX] เพิ่ม x, y, z

    // [FIX] แก้ไข Constructor
    public DeathChestData(UUID ownerUUID, String ownerName, Chest chest, TextDisplay hologramEntity, ItemStack[] items, int experience, String locationString, String worldName, int x, int y, int z) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.chest = chest;
        this.hologramEntity = hologramEntity;
        this.items = items;
        this.experience = experience;
        this.locationString = locationString;
        this.worldName = worldName;
        this.timeLeft = 0;
        this.x = x; // <-- [FIX]
        this.y = y; // <-- [FIX]
        this.z = z; // <-- [FIX]
    }
}