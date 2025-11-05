package xyz.kaijiieow.deathchest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

    private boolean isDeathChest(Location loc) {
        return deathChestManager.getActiveChestAt(loc) != null;
    }

    // 1. กันคนทุบ
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isDeathChest(event.getBlock().getLocation())) {
            // (ถ้าจะให้ OP ทุบได้ ต้องแก้ตรงนี้)
            event.setCancelled(true);
        }
    }

    // 2. กันระเบิด (TNT, Creeper)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isDeathChest(block.getLocation()));
    }

    // 3. กันไฟไหม้
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isDeathChest(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // 4. กัน Piston ดัน
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isDeathChest(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
            if (isDeathChest(block.getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // 5. กัน Piston ดึง
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isDeathChest(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // 6. [อัปเกรด] กัน Hopper (ทั้งบล็อก และ รถ) ดูด
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        // เช็คว่า "ต้นทาง" (Source) คือกล่องศพของเรารึเปล่า
        if (event.getSource().getLocation() != null && isDeathChest(event.getSource().getLocation())) {
            event.setCancelled(true);
        }
    }

    // 7. [ใหม่] กันมังกร/Wither เปลี่ยนบล็อก
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof EnderDragon || event.getEntity() instanceof Wither) {
            if (isDeathChest(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
    
    // 8. [ใหม่] กันฟิสิกส์ (เช่น บล็อกข้างใต้หาย แล้วกล่องเด้ง)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.CHEST) {
            if (isDeathChest(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
}