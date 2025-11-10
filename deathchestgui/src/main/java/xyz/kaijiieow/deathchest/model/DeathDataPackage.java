package xyz.kaijiieow.deathchest.model;

import org.bukkit.inventory.ItemStack;

public class DeathDataPackage {
    
    private final long databaseId;
    private final ItemStack[] items;
    private final int experience;

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

    public long getDatabaseId() {
        return databaseId;
    }
}

