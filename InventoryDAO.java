package com.rebelle.dao;

import com.rebelle.models.InventoryItem;
import com.rebelle.models.InventoryTransaction;
import com.rebelle.models.Category;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * InventoryDAO - Data Access Object for Inventory operations
 */
public class InventoryDAO {
    
    private final Connection connection;
    
    public InventoryDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Create a new inventory item
     */
    public InventoryItem createInventoryItem(InventoryItem item) throws SQLException {
        String sql = """
            INSERT INTO inventory_items (name, category, quantity, unit, threshold, cost_per_unit, 
                                 supplier, expiry_date, notes, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setItemParameters(stmt, item);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating inventory item failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                    return item;
                } else {
                    throw new SQLException("Creating inventory item failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Get inventory item by ID
     */
    public Optional<InventoryItem> getInventoryItemById(int id) throws SQLException {
        String sql = "SELECT * FROM inventory_items WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToItem(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all inventory items
     */
    public List<InventoryItem> getAllInventoryItems() throws SQLException {
        String sql = "SELECT * FROM inventory_items ORDER BY name ASC";
        List<InventoryItem> items = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        }
        
        return items;
    }
    
    /**
     * Get inventory items by category
     */
    public List<InventoryItem> getInventoryItemsByCategory(Category category) throws SQLException {
        String sql = "SELECT * FROM inventory_items WHERE category = ? ORDER BY name ASC";
        List<InventoryItem> items = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, category.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        }
        
        return items;
    }
    
    /**
     * Search inventory items
     */
    public List<InventoryItem> searchInventoryItems(String searchTerm) throws SQLException {
        String sql = """
            SELECT * FROM inventory_items 
            WHERE name LIKE ? OR supplier LIKE ? OR notes LIKE ?
            ORDER BY name ASC
            """;
        
        List<InventoryItem> items = new ArrayList<>();
        String searchPattern = "%" + searchTerm + "%";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        }
        
        return items;
    }
    
    /**
     * Get low stock items
     */
    public List<InventoryItem> getLowStockItems() throws SQLException {
        String sql = "SELECT * FROM inventory_items WHERE quantity <= threshold ORDER BY quantity ASC";
        List<InventoryItem> items = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        }
        
        return items;
    }
    
    /**
     * Get expired items
     */
    public List<InventoryItem> getExpiredItems() throws SQLException {
        String sql = "SELECT * FROM inventory_items WHERE expiry_date IS NOT NULL AND expiry_date < ? ORDER BY expiry_date ASC";
        List<InventoryItem> items = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, LocalDate.now().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        }
        
        return items;
    }
    
    /**
     * Get items expiring soon (within 30 days)
     */
    public List<InventoryItem> getItemsExpiringSoon() throws SQLException {
        String sql = """
            SELECT * FROM inventory_items 
            WHERE expiry_date IS NOT NULL 
            AND expiry_date > ? 
            AND expiry_date <= ?
            ORDER BY expiry_date ASC
            """;
        List<InventoryItem> items = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, LocalDate.now().toString());
            stmt.setString(2, LocalDate.now().plusDays(30).toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        }
        
        return items;
    }
    
    /**
     * Update an existing inventory item
     */
    public boolean updateInventoryItem(InventoryItem item) throws SQLException {
        String sql = """
            UPDATE inventory_items 
            SET name = ?, category = ?, quantity = ?, unit = ?, threshold = ?, cost_per_unit = ?, 
                supplier = ?, expiry_date = ?, notes = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            setItemParameters(stmt, item);
            stmt.setInt(11, item.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete an inventory item
     */
    public boolean deleteInventoryItem(int itemId) throws SQLException {
        // Check if item has transactions
        if (hasTransactions(itemId)) {
            throw new SQLException("Cannot delete inventory item with existing transactions.");
        }
        
        String sql = "DELETE FROM inventory_items WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setInt(1, itemId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Create an inventory transaction
     */
    public InventoryTransaction createInventoryTransaction(InventoryTransaction transaction) throws SQLException {
        String sql = """
            INSERT INTO inventory_transactions (inventory_id, transaction_type, quantity_change, 
                                              reason, appointment_id, transaction_date)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);
            
            stmt.setInt(1, transaction.getInventoryId());
            stmt.setString(2, transaction.getTransactionType().name().toLowerCase());
            stmt.setInt(3, transaction.getQuantityChange());
            stmt.setString(4, transaction.getReason().name().toLowerCase());
            
            if (transaction.getAppointmentId() != null) {
                stmt.setInt(5, transaction.getAppointmentId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            stmt.setString(6, transaction.getTransactionDate().toString());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                connection.rollback();
                throw new SQLException("Creating transaction failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getInt(1));
                } else {
                    connection.rollback();
                    throw new SQLException("Creating transaction failed, no ID obtained.");
                }
            }
            
            // Update inventory quantity
            String updateSql = "UPDATE inventory_items SET quantity = quantity + ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setInt(1, transaction.getQuantityChange());
                updateStmt.setString(2, LocalDateTime.now().toString());
                updateStmt.setInt(3, transaction.getInventoryId());
                
                updateStmt.executeUpdate();
            }
            
            connection.commit();
            return transaction;
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    /**
     * Get transactions for an inventory item
     */
    public List<InventoryTransaction> getTransactionsByInventoryId(int inventoryId) throws SQLException {
        String sql = "SELECT * FROM inventory_transactions WHERE inventory_id = ? ORDER BY transaction_date DESC";
        List<InventoryTransaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setInt(1, inventoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        
        return transactions;
    }
    
    /**
     * Get recent transactions (last 30 days)
     */
    public List<InventoryTransaction> getRecentTransactions(int limit) throws SQLException {
        String sql = """
            SELECT * FROM inventory_transactions 
            WHERE transaction_date >= ?
            ORDER BY transaction_date DESC
            LIMIT ?
            """;
        List<InventoryTransaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, LocalDateTime.now().minusDays(30).toString());
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InventoryTransaction transaction = mapResultSetToTransaction(rs);
                    // Load inventory item
                    Optional<InventoryItem> item = getInventoryItemById(transaction.getInventoryId());
                    item.ifPresent(transaction::setInventoryItem);
                    transactions.add(transaction);
                }
            }
        }
        
        return transactions;
    }
    
    /**
     * Get inventory statistics
     */
    public InventoryStats getInventoryStatistics() throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as total_items,
                SUM(CASE WHEN quantity <= threshold THEN 1 ELSE 0 END) as low_stock_count,
                SUM(CASE WHEN quantity <= 0 THEN 1 ELSE 0 END) as out_of_stock_count,
                SUM(quantity * cost_per_unit) as total_value
            FROM inventory_items
            """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int totalItems = rs.getInt("total_items");
                int lowStockCount = rs.getInt("low_stock_count");
                int outOfStockCount = rs.getInt("out_of_stock_count");
                BigDecimal totalValue = rs.getBigDecimal("total_value");
                
                return new InventoryStats(totalItems, lowStockCount, outOfStockCount, 
                                        totalValue != null ? totalValue : BigDecimal.ZERO);
            }
        }
        
        return new InventoryStats(0, 0, 0, BigDecimal.ZERO);
    }
    
    /**
     * Check if inventory item has transactions
     */
    private boolean hasTransactions(int inventoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory_transactions WHERE inventory_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setInt(1, inventoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to InventoryItem object
     */
    private InventoryItem mapResultSetToItem(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        
        String categoryString = rs.getString("category");
        if (categoryString != null) {
            item.setCategory(Category.valueOf(categoryString.toUpperCase()));
        }
        
        item.setQuantity(rs.getInt("quantity"));
        item.setUnit(rs.getString("unit"));
        item.setThreshold(rs.getInt("threshold"));
        item.setCostPerUnit(rs.getBigDecimal("cost_per_unit"));
        item.setSupplier(rs.getString("supplier"));
        
        String expiryDateString = rs.getString("expiry_date");
        if (expiryDateString != null && !expiryDateString.isEmpty()) {
            item.setExpiryDate(LocalDate.parse(expiryDateString));
        }
        
        item.setNotes(rs.getString("notes"));
        
        String lastUpdatedString = rs.getString("updated_at");
        if (lastUpdatedString != null) {
            item.setUpdatedAt(LocalDateTime.parse(lastUpdatedString));
        }
        
        return item;
    }
    
    /**
     * Map ResultSet to InventoryTransaction object
     */
    private InventoryTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        InventoryTransaction transaction = new InventoryTransaction();
        
        transaction.setId(rs.getInt("id"));
        transaction.setInventoryId(rs.getInt("inventory_id"));
        
        String typeString = rs.getString("transaction_type");
        if (typeString != null) {
            transaction.setTransactionType(InventoryTransaction.TransactionType.valueOf(typeString.toUpperCase()));
        }
        
        transaction.setQuantityChange(rs.getInt("quantity_change"));
        
        String reasonString = rs.getString("reason");
        if (reasonString != null) {
            transaction.setReason(InventoryTransaction.Reason.valueOf(reasonString.toUpperCase()));
        }
        
        int appointmentId = rs.getInt("appointment_id");
        if (!rs.wasNull()) {
            transaction.setAppointmentId(appointmentId);
        }
        
        String transactionDateString = rs.getString("transaction_date");
        if (transactionDateString != null) {
            transaction.setTransactionDate(LocalDateTime.parse(transactionDateString));
        }
        
        return transaction;
    }
    
    /**
     * Inventory statistics class
     */
    public static class InventoryStats {
        private final int totalItems;
        private final int lowStockCount;
        private final int outOfStockCount;
        private final BigDecimal totalValue;
        
        public InventoryStats(int totalItems, int lowStockCount, int outOfStockCount, BigDecimal totalValue) {
            this.totalItems = totalItems;
            this.lowStockCount = lowStockCount;
            this.outOfStockCount = outOfStockCount;
            this.totalValue = totalValue;
        }
        
        public int getTotalItems() { return totalItems; }
        public int getLowStockCount() { return lowStockCount; }
        public int getOutOfStockCount() { return outOfStockCount; }
        public BigDecimal getTotalValue() { return totalValue; }
    }
    
    private void setItemParameters(PreparedStatement stmt, InventoryItem item) throws SQLException {
        stmt.setString(1, item.getName());
        stmt.setString(2, item.getCategory().name());
        stmt.setInt(3, item.getQuantity());
        stmt.setString(4, item.getUnit());
        stmt.setInt(5, item.getThreshold());
        stmt.setBigDecimal(6, item.getCostPerUnit());
        stmt.setString(7, item.getSupplier());
        stmt.setDate(8, item.getExpiryDate() != null ? 
            Date.valueOf(item.getExpiryDate()) : null);
        stmt.setString(9, item.getNotes());
        stmt.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
    }
} 