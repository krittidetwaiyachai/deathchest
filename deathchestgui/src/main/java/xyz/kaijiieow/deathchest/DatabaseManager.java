package xyz.kaijiieow.deathchest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }

            config.setMaximumPoolSize(10);
            config.setPoolName("DeathChestPool");

            this.dataSource = new HikariDataSource(config);
            logger.log(LoggingService.LogLevel.INFO, "เชื่อมต่อ Database (" + dbType + ") สำเร็จ!");

            createTables();

        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "เชื่อมต่อ Database ไม่สำเร็จ! " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    private void createTables() {
        // ตารางสำหรับของใน /buyback
        String buybackTable = "CREATE TABLE IF NOT EXISTS buyback_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "items_base64 TEXT NOT NULL, " +
                "experience INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        // ตารางสำหรับกล่องที่ยัง Active (ยังไม่หมดเวลา)
        String activeChestTable = "CREATE TABLE IF NOT EXISTS active_chests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "world VARCHAR(100) NOT NULL, " +
                "x INT NOT NULL, " +
                "y INT NOT NULL, " +
                "z INT NOT NULL, " +
                "items_base64 TEXT NOT NULL, " +
                "experience INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = getConnection();
             PreparedStatement ps1 = conn.prepareStatement(buybackTable);
             PreparedStatement ps2 = conn.prepareStatement(activeChestTable)) {
            
            ps1.execute();
            ps2.execute();
            logger.log(LoggingService.LogLevel.INFO, "ตรวจสอบ/สร้างตาราง Database เรียบร้อย");

        } catch (SQLException e) {
            logger.log(LoggingService.LogLevel.ERROR, "ไม่สามารถสร้างตาราง Database ได้: " + e.getMessage());
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
}