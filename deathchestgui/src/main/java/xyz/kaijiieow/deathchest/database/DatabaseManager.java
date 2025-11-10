package xyz.kaijiieow.deathchest.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import xyz.kaijiieow.deathchest.manager.ConfigManager;
import xyz.kaijiieow.deathchest.plugin.DeathChestPlugin;
import xyz.kaijiieow.deathchest.util.LoggingService;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final DeathChestPlugin plugin;
    private final ConfigManager configManager;
    private final LoggingService logger;
    private HikariDataSource dataSource;
    
    private ChestDatabase chestDatabase;
    private BuybackDatabase buybackDatabase;

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

            this.chestDatabase = new ChestDatabase(dataSource, logger);
            this.buybackDatabase = new BuybackDatabase(dataSource, logger);
            
            createTables();

        } catch (Exception e) {
            logger.log(LoggingService.LogLevel.ERROR, "เชื่อมต่อ Database ไม่สำเร็จ! " + e.getMessage());
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private void createTables() {
        chestDatabase.createTables();
        buybackDatabase.createTables();
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
            plugin.getLogger().info("ปิดการเชื่อมต่อ Database pool");
        }
    }

    public ChestDatabase getChestDatabase() {
        return chestDatabase;
    }
    
    public BuybackDatabase getBuybackDatabase() {
        return buybackDatabase;
    }
}

