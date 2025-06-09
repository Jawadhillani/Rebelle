package com.rebelle.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.InputStream;
import java.util.Scanner;

/**
 * DatabaseManager - Singleton class for managing SQLite database connection
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_NAME = "rebelle_medical.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;
    
    private DatabaseManager() {
        // Private constructor for singleton pattern
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Initialize database connection and create tables
     */
    public void initializeDatabase() throws SQLException {
        try {
            // Ensure the database directory exists
            createDatabaseDirectory();
            
            // Create connection
            connection = DriverManager.getConnection(DB_URL);
            
            // Enable foreign key constraints
            connection.createStatement().execute("PRAGMA foreign_keys = ON;");
            
            // Execute schema creation
            createTables();
            
            System.out.println("Database initialized successfully: " + DB_NAME);
            
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get database connection
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initializeDatabase();
        }
        return connection;
    }
    
    /**
     * Close database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
    
    /**
     * Create database directory if it doesn't exist
     */
    private void createDatabaseDirectory() {
        try {
            Path dbPath = Paths.get(DB_NAME);
            Path parentDir = dbPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
        } catch (IOException e) {
            System.err.println("Failed to create database directory: " + e.getMessage());
        }
    }
    
    /**
     * Create database tables using schema.sql
     */
    private void createTables() throws SQLException {
        try (InputStream schemaStream = getClass().getResourceAsStream("/database/schema.sql")) {
            
            if (schemaStream == null) {
                throw new SQLException("Schema file not found in resources");
            }
            
            // Read schema file
            Scanner scanner = new Scanner(schemaStream);
            scanner.useDelimiter(";");
            
            Statement statement = connection.createStatement();
            
            while (scanner.hasNext()) {
                String sql = scanner.next().trim();
                if (!sql.isEmpty() && !sql.startsWith("--")) {
                    try {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        System.err.println("Error executing SQL: " + sql);
                        System.err.println("Error: " + e.getMessage());
                        // Continue with other statements
                    }
                }
            }
            
            statement.close();
            scanner.close();
            
            System.out.println("Database schema created successfully.");
            
        } catch (IOException e) {
            throw new SQLException("Failed to read schema file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute a simple query to verify database is working
     */
    public boolean verifyDatabase() {
        try {
            Statement stmt = getConnection().createStatement();
            stmt.execute("SELECT COUNT(*) FROM patients");
            stmt.close();
            return true;
        } catch (SQLException e) {
            System.err.println("Database verification failed: " + e.getMessage());
            return false;
        }
    }
} 