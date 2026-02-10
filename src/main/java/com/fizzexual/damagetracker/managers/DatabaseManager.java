package com.fizzexual.damagetracker.managers;

import com.fizzexual.damagetracker.DamageTracker;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final DamageTracker plugin;
    private Connection connection;

    public DatabaseManager(DamageTracker plugin) {
        this.plugin = plugin;
        this.initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            String dbType = plugin.getConfig().getString("database.type", "sqlite");
            
            if ("sqlite".equalsIgnoreCase(dbType)) {
                initializeSQLite();
            } else if ("mysql".equalsIgnoreCase(dbType)) {
                initializeMySQL();
            } else {
                plugin.getLogger().warning("Unknown database type: " + dbType + ". Using SQLite.");
                initializeSQLite();
            }

            createTables();
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Could not initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeSQLite() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        String fileName = plugin.getConfig().getString("database.sqlite.file", "etherealdrops.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" +
                new File(plugin.getDataFolder(), fileName).getAbsolutePath());
        plugin.getLogger().info("Connected to SQLite database: " + fileName);
    }

    private void initializeMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "etherealdrops");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "password");
        boolean useSSL = plugin.getConfig().getBoolean("database.mysql.useSSL", false);

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s", host, port, database, useSSL);
        connection = DriverManager.getConnection(url, username, password);
        plugin.getLogger().info("Connected to MySQL database: " + database);
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS boss_damage (
                    boss_name TEXT,
                    player_uuid TEXT,
                    player_name TEXT,
                    damage DOUBLE,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (boss_name, player_uuid)
                )
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    public void updateDamage(String bossName, UUID playerUuid, String playerName, double damage) {
        if (bossName == null || bossName.trim().isEmpty()) {
            plugin.getLogger().warning("Attempted to update damage with null or empty boss name");
            return;
        }
        String sql = """
            INSERT OR REPLACE INTO boss_damage (boss_name, player_uuid, player_name, damage, last_updated)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bossName.toUpperCase());
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, playerName);
            pstmt.setDouble(4, damage);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update damage: " + e.getMessage());
        }
    }

    public String getFormattedLeaderboard(String bossName) {
        String sql = """
            SELECT player_name, damage
            FROM boss_damage
            WHERE boss_name = ?
            ORDER BY damage DESC
            LIMIT 10
        """;

        StringBuilder result = new StringBuilder();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bossName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();

            int position = 1;
            while (rs.next()) {
                String playerName = rs.getString("player_name");
                double damage = rs.getDouble("damage");

                result.append(String.format("#%d %s: %.0f\n", position, playerName, damage));
                position++;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get leaderboard: " + e.getMessage());
            return "Error loading leaderboard";
        }

        return result.toString().trim();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not close database connection: " + e.getMessage());
        }
    }
}
