package xyz.kaijiieow.deathchest.database;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;
import xyz.kaijiieow.deathchest.model.DatabaseBuybackData;
import xyz.kaijiieow.deathchest.util.LoggingService;
import xyz.kaijiieow.deathchest.util.SerializationUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuybackDatabase {
    
    private final HikariDataSource dataSource;
    private final LoggingService logger;
    
    public BuybackDatabase(HikariDataSource dataSource, LoggingService logger) {
        this.dataSource = dataSource;
        this.logger = logger;
    }
    
    public void createTables() {
        String buybackTable = "CREATE TABLE IF NOT EXISTS buyback_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "items_base64 TEXT NOT NULL, " +
                "experience INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(buybackTable);

            logger.log(LoggingService.LogLevel.INFO, "ตรวจสอบ/สร้างตาราง Database เรียบร้อย");

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถสร้างตาราง Database ได้: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public List<DatabaseBuybackData> loadAllBuybackItems() {
        List<DatabaseBuybackData> items = new ArrayList<>();
        String sql = "SELECT id, owner_uuid, items_base64, experience FROM buyback_items";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                items.add(new DatabaseBuybackData(
                    rs.getLong("id"),
                    rs.getString("owner_uuid"),
                    rs.getString("items_base64"),
                    rs.getInt("experience")
                ));
            }
        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถโหลด Buyback Items จาก DB: " + e.getMessage());
        }
        return items;
    }
    
    public long saveBuybackItem(UUID ownerUuid, ItemStack[] items, int experience) {
        String sql = "INSERT INTO buyback_items (owner_uuid, items_base64, experience) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            String itemsBase64 = SerializationUtils.itemStackArrayToBase64(items);
            
            ps.setString(1, ownerUuid.toString());
            ps.setString(2, itemsBase64);
            ps.setInt(3, experience);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถเซฟ Buyback Item ลง DB ได้: " + e.getMessage());
        }
        return -1;
    }
    
    public void deleteBuybackItem(long databaseId) {
        String sql = "DELETE FROM buyback_items WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, databaseId);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถลบ Buyback Item ออกจาก DB ได้: " + e.getMessage());
        }
    }
}

