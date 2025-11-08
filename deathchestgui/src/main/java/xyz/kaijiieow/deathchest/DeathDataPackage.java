package xyz.kaijiieow.deathchest;

import org.bukkit.inventory.ItemStack;

public class DeathDataPackage {
    
    final long databaseId; // [NEW] ID จากตาราง buyback_items
    final ItemStack[] items;
    final int experience;

    public DeathDataPackage(long databaseId, ItemStack[] items, int experience) {
        this.databaseId = databaseId;
        this.items = items;
        this.experience = experience;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public int getExperience() {
        return experience;
    }

    // [NEW] Getter for DB ID
    public long getDatabaseId() {
        return databaseId;
    }
}