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
    int timeLeft; // <-- [FIX] เพิ่ม field นี้

    public DeathChestData(UUID ownerUUID, String ownerName, Chest chest, TextDisplay hologramEntity, ItemStack[] items, int experience, String locationString) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.chest = chest;
        this.hologramEntity = hologramEntity;
        this.items = items;
        this.experience = experience;
        this.locationString = locationString;
        this.timeLeft = 0; // <-- [FIX] เพิ่ม
    }
}