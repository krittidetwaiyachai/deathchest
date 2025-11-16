package xyz.kaijiieow.deathchest.model;

import org.bukkit.entity.TextDisplay;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DeathChestData {
    public UUID ownerUUID;
    public String ownerName;
    public Chest chest;
    public TextDisplay hologramEntity;
    public ItemStack[] items;
    public int experience;
    public String locationString;
    public int timeLeft;
    public int initialDespawnTime; // เวลา despawn เริ่มต้น (วินาที)
    public String worldName;
    public int x, y, z;
    public long createdAt;

    public DeathChestData(UUID ownerUUID, String ownerName, Chest chest, TextDisplay hologramEntity,
                          ItemStack[] items, int experience, String locationString, String worldName,
                          int x, int y, int z, long createdAt, int initialDespawnTime) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.chest = chest;
        this.hologramEntity = hologramEntity;
        this.items = items;
        this.experience = experience;
        this.locationString = locationString;
        this.worldName = worldName;
        this.timeLeft = 0;
        this.initialDespawnTime = initialDespawnTime;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAt = createdAt;
    }
}

