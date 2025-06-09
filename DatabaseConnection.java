package com.rebelle.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection - Singleton class for managing database connections
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rebelle_medical";
    private static final String USER = "root";
    private static final String PASS = "";
    
    private DatabaseConnection() {
        // Private constructor to enforce singleton pattern
    }
    
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }
} 