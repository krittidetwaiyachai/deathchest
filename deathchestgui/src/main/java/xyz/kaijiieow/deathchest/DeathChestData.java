package xyz.kaijiieow.deathchest;

import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

// Class ช่วยเก็บข้อมูล
public class DeathChestData {
    UUID ownerUUID;
    String ownerName;
    Chest chest;
    Hologram hologram;
    ItemStack[] items;

    public DeathChestData(UUID ownerUUID, String ownerName, Chest chest, Hologram hologram, ItemStack[] items) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.chest = chest;
        this.hologram = hologram;
        this.items = items;
    }
}