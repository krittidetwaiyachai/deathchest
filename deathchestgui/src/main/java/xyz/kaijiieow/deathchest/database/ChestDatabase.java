package xyz.kaijiieow.deathchest.database;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;
import xyz.kaijiieow.deathchest.model.BlockLocation;
import xyz.kaijiieow.deathchest.model.DatabaseChestData;
import xyz.kaijiieow.deathchest.model.DeathChestData;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.util.SerializationUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChestDatabase {
    
    private final HikariDataSource dataSource;
    private final LoggingService logger;
    
    public ChestDatabase(HikariDataSource dataSource, LoggingService logger) {
        this.dataSource = dataSource;
        this.logger = logger;
    }
    
    public void createTables() {
        String activeChestTable = "CREATE TABLE IF NOT EXISTS active_chests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "owner_name VARCHAR(36) NOT NULL DEFAULT 'Unknown', " +
                "world VARCHAR(100) NOT NULL, " +
                "x INT NOT NULL, " +
                "y INT NOT NULL, " +
                "z INT NOT NULL, " +
                "items_base64 TEXT NOT NULL, " +
                "experience INT NOT NULL, " +
                "remaining_seconds INT NOT NULL, " +
                "created_at BIGINT NOT NULL DEFAULT 0" +
                ");";
        
        String activeChestIndex = "CREATE INDEX IF NOT EXISTS idx_active_chests_coords ON active_chests (world, x, y, z);";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(activeChestTable);
            stmt.execute(activeChestIndex);

            logger.log(LoggingService.LogLevel.INFO, "ตรวจสอบ/สร้างตาราง Database เรียบร้อย");

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถสร้างตาราง Database ได้: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public List<DatabaseChestData> loadAllActiveChests() {
        List<DatabaseChestData> chests = new ArrayList<>();
        String sql = "SELECT owner_uuid, owner_name, world, x, y, z, items_base64, experience, remaining_seconds, created_at FROM active_chests";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int remainingSeconds = rs.getInt("remaining_seconds");
                long createdAt = rs.getLong("created_at");
                
                chests.add(new DatabaseChestData(
                    rs.getString("owner_uuid"),
                    rs.getString("owner_name"),
                    rs.getString("world"),
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("z"),
                    rs.getString("items_base64"),
                    rs.getInt("experience"),
                    remainingSeconds,
                    createdAt
                ));
            }
        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถโหลด Active Chests จาก DB: " + e.getMessage());
        }
        return chests;
    }
    
    public void saveActiveChest(DeathChestData data, int remainingTime, long createdAt) {
        String sql = "INSERT INTO active_chests (owner_uuid, owner_name, world, x, y, z, items_base64, experience, remaining_seconds, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String itemsBase64 = SerializationUtils.itemStackArrayToBase64(data.items);

            ps.setString(1, data.ownerUUID.toString());
            ps.setString(2, data.ownerName);
            ps.setString(3, data.worldName);
            ps.setInt(4, data.x);
            ps.setInt(5, data.y);
            ps.setInt(6, data.z);
            ps.setString(7, itemsBase64);
            ps.setInt(8, data.experience);
            ps.setInt(9, remainingTime);
            ps.setLong(10, createdAt);
            ps.executeUpdate();

        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถเซฟ Active Chest ลง DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void deleteActiveChest(Location loc) {
        String sql = "DELETE FROM active_chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถลบ Active Chest ออกจาก DB: " + e.getMessage());
        }
    }
    
    public void batchUpdateChestTimes(Map<BlockLocation, DeathChestData> activeChests) {
        String sql = "UPDATE active_chests SET remaining_seconds = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);

            for (Map.Entry<BlockLocation, DeathChestData> entry : activeChests.entrySet()) {
                BlockLocation key = entry.getKey();
                DeathChestData data = entry.getValue();

                if (data != null) {
                    ps.setInt(1, data.timeLeft);
                    ps.setString(2, key.worldName());
                    ps.setInt(3, key.x());
                    ps.setInt(4, key.y());
                    ps.setInt(5, key.z());
                    ps.addBatch();
                }
            }
            
            int[] results = ps.executeBatch();
            conn.commit();
            
            logger.log(LoggingService.LogLevel.INFO, "Batch update เวลาคงเหลือของกล่องศพ " + results.length + " กล่อง สำเร็จ.");

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถ batch update เวลาคงเหลือของกล่องศพได้: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

