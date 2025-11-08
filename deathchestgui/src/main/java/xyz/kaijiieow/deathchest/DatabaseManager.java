package xyz.kaijiieow.deathchest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
// import java.sql.Timestamp; // ไม่ใช้แล้ว
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final LoggingService logger;
    private HikariDataSource dataSource;

    public DatabaseManager(DeathChestPlugin plugin, ConfigManager configManager, LoggingService logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
    }

    public void connect() {
        try {
            HikariConfig config = new HikariConfig();
            String dbType = configManager.getDbType();

            if (dbType.equals("SQLITE")) {
                File dbFile = new File(plugin.getDataFolder(), configManager.getDbFilename());
                if (!dbFile.exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                config.setDriverClassName("org.sqlite.JDBC");
            } else if (dbType.equals("MYSQL")) {
                config.setJdbcUrl("jdbc:mysql://" + configManager.getDbHost() + ":" + configManager.getDbPort() + "/" + configManager.getDbDatabase());
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setUsername(configManager.getDbUsername());
                config.setPassword(configManager.getDbPassword());
            } else {
                logger.log(LoggingService.LogLevel.ERROR, "Invalid database type '" + dbType + "' in config.yml. Disabling plugin.");
                Bukkit.getPluginManager().disablePlugin(plugin); // <-- Error อยู่ตรงนี้
                return;
            }

            config.setMaximumPoolSize(10);
            config.setPoolName("DeathChestPool");

            this.dataSource = new HikariDataSource(config);
            logger.log(LoggingService.LogLevel.INFO, "เชื่อมต่อ Database (" + dbType + ") สำเร็จ!");

            createTables();

        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "เชื่อมต่อ Database ไม่สำเร็จ! " + e.getMessage());
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private void createTables() {
        String buybackTable = "CREATE TABLE IF NOT EXISTS buyback_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "items_base64 TEXT NOT NULL, " +
                "experience INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String activeChestTable = "CREATE TABLE IF NOT EXISTS active_chests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "world VARCHAR(100) NOT NULL, " +
                "x INT NOT NULL, " +
                "y INT NOT NULL, " +
                "z INT NOT NULL, " +
                "items_base64 TEXT NOT NULL, " +
                "experience INT NOT NULL, " +
                "remaining_seconds INT NOT NULL" + 
                ");";
        
        String activeChestIndex = "CREATE INDEX IF NOT EXISTS idx_active_chests_coords ON active_chests (world, x, y, z);";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(buybackTable);
            stmt.execute(activeChestTable);
            stmt.execute(activeChestIndex); 

            logger.log(LoggingService.LogLevel.INFO, "ตรวจสอบ/สร้างตาราง Database เรียบร้อย");

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถสร้างตาราง Database ได้: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection is not initialized.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.log(LoggingService.LogLevel.INFO, "ปิดการเชื่อมต่อ Database pool");
        }
    }

    public List<DatabaseChestData> loadAllActiveChests() {
        List<DatabaseChestData> chests = new ArrayList<>();
        String sql = "SELECT owner_uuid, world, x, y, z, items_base64, experience, remaining_seconds FROM active_chests"; 
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int remainingSeconds = rs.getInt("remaining_seconds");
                
                chests.add(new DatabaseChestData(
                    rs.getString("owner_uuid"),
                    rs.getString("world"),
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("z"),
                    rs.getString("items_base64"),
                    rs.getInt("experience"),
                    remainingSeconds 
                ));
            }
        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถโหลด Active Chests จาก DB: " + e.getMessage());
        }
        return chests;
    }

    // --- [FIX] แก้ไข Method นี้ ---
    public void saveActiveChest(DeathChestData data, int remainingTime) { 
        String sql = "INSERT INTO active_chests (owner_uuid, world, x, y, z, items_base64, experience, remaining_seconds) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"; 
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String itemsBase64 = SerializationUtils.itemStackArrayToBase64(data.items);
            // Location loc = data.chest.getLocation(); // <-- [FIX] ลบทิ้ง ไม่ใช้ API

            ps.setString(1, data.ownerUUID.toString());
            ps.setString(2, data.worldName); // <-- [FIX] ใช้ค่า primitive จาก data
            ps.setInt(3, data.x); // <-- [FIX] ใช้ค่า primitive จาก data
            ps.setInt(4, data.y); // <-- [FIX] ใช้ค่า primitive จาก data
            ps.setInt(5, data.z); // <-- [FIX] ใช้ค่า primitive จาก data
            ps.setString(6, itemsBase64);
            ps.setInt(7, data.experience);
            ps.setInt(8, remainingTime); 
            ps.executeUpdate();

        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถเซฟ Active Chest ลง DB: " + e.getMessage());
            e.printStackTrace(); // เพิ่ม stacktrace
        }
    }
    // ----------------------------------------------------

    public void deleteActiveChest(Location loc) {
        String sql = "DELETE FROM active_chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (Connection conn = getConnection();
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

    public void updateChestTime(int x, int y, int z, int remainingSeconds, String worldName) {
        logger.log(LoggingService.LogLevel.INFO, String.format("กำลังอัปเดตเวลา: W=%s, X=%d, Y=%d, Z=%d, Time=%d", worldName, x, y, z, remainingSeconds));
        
        String sql = "UPDATE active_chests SET remaining_seconds = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, remainingSeconds);
            ps.setString(2, worldName); 
            ps.setInt(3, x); 
            ps.setInt(4, y); 
            ps.setInt(5, z); 
            int rowsAffected = ps.executeUpdate(); 

            if (rowsAffected == 0) {
                 logger.log(LoggingService.LogLevel.WARN, String.format("UpdateChestTime ไม่พบแถวที่จะอัปเดต: W=%s, X=%d, Y=%d, Z=%d", worldName, x, y, z));
            } else {
                 logger.log(LoggingService.LogLevel.INFO, String.format("UpdateChestTime อัปเดต %d แถว สำเร็จ", rowsAffected)); // เพิ่ม log ตอนสำเร็จ
            }

        } catch (Exception e) { 
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถอัปเดตเวลา Active Chest ใน DB: " + e.getMessage());
            e.printStackTrace(); 
        }
    }


    public List<DatabaseBuybackData> loadAllBuybackItems() {
        List<DatabaseBuybackData> items = new ArrayList<>();
        String sql = "SELECT id, owner_uuid, items_base64, experience FROM buyback_items";
        try (Connection conn = getConnection();
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
        try (Connection conn = getConnection();
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
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถเซฟ Buyback Item ลง DB: " + e.getMessage());
        }
        return -1;
    }

    public void deleteBuybackItem(long databaseId) {
        String sql = "DELETE FROM buyback_items WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, databaseId);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถลบ Buyback Item ออกจาก DB: " + e.getMessage());
        }
    }
}