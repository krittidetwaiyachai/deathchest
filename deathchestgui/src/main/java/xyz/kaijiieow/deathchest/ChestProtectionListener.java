package xyz.kaijiieow.deathchest;

import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

public class ChestProtectionListener implements Listener {

    private final DeathChestManager deathChestManager;

    public ChestProtectionListener(DeathChestManager deathChestManager) {
        this.deathChestManager = deathChestManager;
    }

    private boolean isDeathChest(Block block) {
        if (block == null) return false;
        // เรียกเมธอดใหม่ (ที่แก้ใน Phase 3) ที่รับ Block
        return deathChestManager.getActiveChestAt(block) != null;
    }

    // 1. กันคนทุบ
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isDeathChest(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // 2. กันระเบิด (TNT, Creeper)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isDeathChest);
    }

    // 3. กันไฟไหม้
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isDeathChest(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    // 4. กัน Piston ดัน
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isDeathChest(block)) {
                event.setCancelled(true);
                return;
            }
            if (isDeathChest(block.getRelative(event.getDirection()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // 5. กัน Piston ดึง
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isDeathChest(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // 6. [อัปเกรด] กัน Hopper (ทั้งบล็อก และ รถ) ดูด
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (event.getSource().getLocation() != null) {
            if (isDeathChest(event.getSource().getLocation().getBlock())) {
                event.setCancelled(true);
            }
        }
    }

    // 7. [ใหม่] กันมังกร/Wither เปลี่ยนบล็อก
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof EnderDragon || event.getEntity() instanceof Wither) {
            if (isDeathChest(event.getBlock())) {
                event.setCancelled(true);
            }
        }
    }
    
    // 8. [ใหม่] กันฟิสิกส์ (เช่น บล็อกข้างใต้หาย แล้วกล่องเด้ง)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.CHEST) {
            if (isDeathChest(event.getBlock())) {
                event.setCancelled(true);
            }
        }
    }
}