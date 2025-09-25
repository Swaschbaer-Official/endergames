package org.swaschbaer.endergames.config;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.swaschbaer.endergames.Main;

import java.sql.*;

public class DataHandler {
    private String ip = Main.getInstance().getConfig().getString("mysql.ip");
    private String port = Main.getInstance().getConfig().getString("mysql.port");
    private String database = Main.getInstance().getConfig().getString("mysql.database");
    private String username = Main.getInstance().getConfig().getString("mysql.username");
    private String password = Main.getInstance().getConfig().getString("mysql.password");
    private Connection connection;

    private Plugin plugin;

    public DataHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private void checkConnection() {
        if (!isConnected()) {
            Bukkit.getLogger().warning("Datenbankverbindung verloren. Versuche, erneut zu verbinden...");
            connect();
        }
    }

    // Verbindung zur Datenbank herstellen
    public void connect() {
        try {
            String url = "jdbc:mysql://" + ip + ":" + port + "/" + database + "?autoReconnect=true";
            connection = DriverManager.getConnection(url, username, password);
            Bukkit.getServer().getLogger().info("Verbindung erfolgreich hergestellt.");
        } catch (Exception e) {
            System.err.println("Verbindungsfehler: " + e.getMessage());
        }
    }

    public String getValue(String table, String column, String condition) {
        checkConnection();
        String result = null;
        String query = "SELECT " + column + " FROM " + table + " WHERE " + condition + ";";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                result = rs.getString(column);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Fehler beim Abrufen des Werts: " + e.getMessage());
        }

        return result;
    }

    public boolean columnExists(String table, String column, String uuid) {
        checkConnection();
        String query = "SELECT 1 FROM " + table + " WHERE UUID = ? LIMIT 1;";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Fehler beim Prüfen der Spalte: " + e.getMessage());
            return false;
        }
    }

    public void createTable(String tableName, String columns) {
        checkConnection();
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
            Bukkit.getLogger().info("Tabelle " + tableName + " wurde erfolgreich erstellt.");
        } catch (Exception e) {
            Bukkit.getLogger().warning("Fehler beim Erstellen der Tabelle: " + e.getMessage());
        }
    }

    public void setLanguage(String playerUUID, String languageFile) {
        checkConnection();

        try {
            String checkQuery = "SELECT COUNT(*) FROM language WHERE uuid = ?;";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, playerUUID);
                try (ResultSet resultSet = checkStmt.executeQuery()) {
                    if (resultSet.next() && resultSet.getInt(1) == 0) {
                        String insertQuery = "INSERT INTO language (uuid, language_file) VALUES (?, ?);";
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                            insertStmt.setString(1, playerUUID);
                            insertStmt.setString(2, "EN_en");
                            insertStmt.executeUpdate();
                            System.out.println("Neue Spracheintragung für Spieler erstellt.");
                        }
                    } else {
                        String updateQuery = "UPDATE language SET language_file = ? WHERE uuid = ?;";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, languageFile);
                            updateStmt.setString(2, playerUUID);
                            updateStmt.executeUpdate();
                            System.out.println("Spracheintragung aktualisiert.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen der Sprachdatei: " + e.getMessage());
        }
    }

    public String getLanguage(String playerUUID) {
        checkConnection();
        String result = null;
        String query = "SELECT language_file FROM language WHERE uuid = ?;";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUUID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result = rs.getString("language_file");
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Sprachdatei: " + e.getMessage());
        }

        return result;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                Bukkit.getLogger().info("Verbindung geschlossen.");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Fehler beim Schließen der Verbindung: " + e.getMessage());
        }
    }
}
