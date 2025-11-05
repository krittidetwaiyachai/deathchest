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

    public DeathChestData(UUID ownerUUID, String ownerName, Chest chest, TextDisplay hologramEntity, ItemStack[] items) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.chest = chest;
        this.hologramEntity = hologramEntity;
        this.items = items;
    }
}