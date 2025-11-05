package xyz.kaijiieow.deathchest;

import org.bukkit.inventory.ItemStack;

public class DeathDataPackage {
    
    final ItemStack[] items;
    final int experience;

    public DeathDataPackage(ItemStack[] items, int experience) {
        this.items = items;
        this.experience = experience;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public int getExperience() {
        return experience;
    }
}