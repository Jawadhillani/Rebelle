package com.rebelle.dao;

import com.rebelle.models.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Data Access Object for Service operations
 */
public class ServiceDAO {
    
    private final DatabaseManager dbManager;
    
    public ServiceDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Create a new service
     */
    public Optional<Service> create(Service service) {
        String sql = "INSERT INTO services (name, description, duration_minutes, price, created_at) " +
                    "VALUES (?, ?, ?, ?, ?)";
                    
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, service.getName());
            stmt.setString(2, service.getDescription());
            stmt.setInt(3, service.getDurationMinutes());
            stmt.setBigDecimal(4, service.getDefaultPrice());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return Optional.empty();
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    service.setId(generatedKeys.getInt(1));
                    return Optional.of(service);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Optional.empty();
    }
    
    /**
     * Update an existing service
     */
    public boolean update(Service service) {
        String sql = "UPDATE services SET name = ?, description = ?, duration_minutes = ?, " +
                    "price = ?, updated_at = ? WHERE id = ?";
                    
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, service.getName());
            stmt.setString(2, service.getDescription());
            stmt.setInt(3, service.getDurationMinutes());
            stmt.setBigDecimal(4, service.getDefaultPrice());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(6, service.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a service
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM services WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Find service by ID
     */
    public Optional<Service> getServiceById(int id) {
        String sql = "SELECT * FROM services WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToService(rs));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Optional.empty();
    }
    
    /**
     * Find all services
     */
    public List<Service> findAll() {
        String sql = "SELECT * FROM services ORDER BY name";
        List<Service> services = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                services.add(mapResultSetToService(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return services;
    }
    
    /**
     * Find services by name (partial match)
     */
    public List<Service> findByName(String name) {
        String sql = "SELECT * FROM services WHERE LOWER(name) LIKE LOWER(?) ORDER BY name";
        List<Service> services = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + name + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    services.add(mapResultSetToService(rs));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return services;
    }
    
    /**
     * Map ResultSet to Service object
     */
    private Service mapResultSetToService(ResultSet rs) throws SQLException {
        Service service = new Service();
        service.setId(rs.getInt("id"));
        service.setName(rs.getString("name"));
        service.setDescription(rs.getString("description"));
        service.setDurationMinutes(rs.getInt("duration_minutes"));
        service.setDefaultPrice(rs.getBigDecimal("price"));
        service.setActive(rs.getBoolean("is_active"));
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            service.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return service;
    }
    
    /**
     * Get all active services
     */
    public List<Service> getAllActiveServices() {
        String sql = "SELECT * FROM services WHERE is_active = true ORDER BY name";
        List<Service> services = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                services.add(mapResultSetToService(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return services;
    }
} 