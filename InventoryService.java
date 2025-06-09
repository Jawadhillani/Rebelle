package com.rebelle.services;

import com.rebelle.dao.InventoryDAO;
import com.rebelle.models.InventoryItem;
import com.rebelle.models.InventoryTransaction;
import com.rebelle.models.Category;
import com.rebelle.dao.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * InventoryService - Business logic layer for inventory operations
 */
public class InventoryService {
    
    private final InventoryDAO inventoryDAO;
    
    public InventoryService() {
        try {
            Connection connection = DatabaseManager.getInstance().getConnection();
            this.inventoryDAO = new InventoryDAO(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize InventoryService", e);
        }
    }
    
    /**
     * Create a new inventory item with validation
     */
    public ServiceResult<InventoryItem> createInventoryItem(String name, Category category,
                                                          int quantity, String unit, int threshold,
                                                          BigDecimal costPerUnit, String supplier,
                                                          LocalDate expiryDate, String notes) {
        try {
            // Validate input
            ValidationResult validation = validateInventoryItemData(name, category, quantity, unit, 
                                                                  threshold, costPerUnit, expiryDate, null);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Create inventory item
            InventoryItem item = new InventoryItem();
            item.setName(name);
            item.setCategory(category);
            item.setQuantity(quantity);
            item.setUnit(unit);
            item.setThreshold(threshold);
            item.setCostPerUnit(costPerUnit != null ? costPerUnit : BigDecimal.ZERO);
            item.setSupplier(supplier);
            item.setExpiryDate(expiryDate);
            item.setNotes(notes);
            
            InventoryItem createdItem = inventoryDAO.createInventoryItem(item);
            
            // Create initial stock transaction if quantity > 0
            if (quantity > 0) {
                InventoryTransaction transaction = new InventoryTransaction(
                    createdItem.getId(),
                    quantity,
                    InventoryTransaction.Reason.RESTOCK,
                    "Initial stock"
                );
                inventoryDAO.createInventoryTransaction(transaction);
            }
            
            return ServiceResult.success(createdItem, "Inventory item created successfully.");
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing inventory item
     */
    public ServiceResult<InventoryItem> updateInventoryItem(int itemId, String name, Category category,
                                                          String unit, int threshold, BigDecimal costPerUnit,
                                                          String supplier, LocalDate expiryDate, String notes) {
        try {
            // Check if item exists
            Optional<InventoryItem> existingItem = inventoryDAO.getInventoryItemById(itemId);
            if (existingItem.isEmpty()) {
                return ServiceResult.error("Inventory item not found.");
            }
            
            // Validate input (don't validate quantity here as it's managed through transactions)
            ValidationResult validation = validateInventoryItemData(name, category, existingItem.get().getQuantity(), 
                                                                  unit, threshold, costPerUnit, expiryDate, itemId);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Update item data (don't change quantity directly)
            InventoryItem item = existingItem.get();
            item.setName(name.trim());
            item.setCategory(category);
            item.setUnit(unit != null ? unit.trim() : "pieces");
            item.setThreshold(threshold);
            item.setCostPerUnit(costPerUnit != null ? costPerUnit : BigDecimal.ZERO);
            item.setSupplier(supplier != null ? supplier.trim() : null);
            item.setExpiryDate(expiryDate);
            item.setNotes(notes != null ? notes.trim() : null);
            
            boolean updated = inventoryDAO.updateInventoryItem(item);
            if (updated) {
                return ServiceResult.success(item, "Inventory item updated successfully.");
            } else {
                return ServiceResult.error("Failed to update inventory item.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Add stock to inventory item
     */
    public ServiceResult<InventoryItem> addStock(int itemId, int quantity, String reason, String notes) {
        try {
            // Check if item exists
            Optional<InventoryItem> existingItem = inventoryDAO.getInventoryItemById(itemId);
            if (existingItem.isEmpty()) {
                return ServiceResult.error("Inventory item not found.");
            }
            
            if (quantity <= 0) {
                return ServiceResult.error("Quantity to add must be greater than 0.");
            }
            
            // Create transaction
            InventoryTransaction transaction = new InventoryTransaction(
                itemId,
                quantity,
                InventoryTransaction.Reason.RESTOCK,
                notes
            );
            
            inventoryDAO.createInventoryTransaction(transaction);
            
            // Get updated item
            Optional<InventoryItem> updatedItem = inventoryDAO.getInventoryItemById(itemId);
            if (updatedItem.isPresent()) {
                return ServiceResult.success(updatedItem.get(), 
                    String.format("Added %d %s to %s", quantity, updatedItem.get().getUnit(), updatedItem.get().getName()));
            } else {
                return ServiceResult.error("Failed to retrieve updated item.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Remove stock from inventory item
     */
    public ServiceResult<InventoryItem> removeStock(int itemId, int quantity, InventoryTransaction.Reason reason, 
                                                   Integer appointmentId, String notes) {
        try {
            // Check if item exists
            Optional<InventoryItem> existingItem = inventoryDAO.getInventoryItemById(itemId);
            if (existingItem.isEmpty()) {
                return ServiceResult.error("Inventory item not found.");
            }
            
            if (quantity <= 0) {
                return ServiceResult.error("Quantity to remove must be greater than 0.");
            }
            
            InventoryItem item = existingItem.get();
            if (item.getQuantity() < quantity) {
                return ServiceResult.error(
                    String.format("Insufficient stock. Available: %d %s, Requested: %d %s", 
                                item.getQuantity(), item.getUnit(), quantity, item.getUnit()));
            }
            
            // Create transaction
            InventoryTransaction transaction = new InventoryTransaction(
                itemId,
                -quantity, // Negative for removal
                reason,
                notes
            );
            transaction.setAppointmentId(appointmentId);
            
            inventoryDAO.createInventoryTransaction(transaction);
            
            // Get updated item
            Optional<InventoryItem> updatedItem = inventoryDAO.getInventoryItemById(itemId);
            if (updatedItem.isPresent()) {
                return ServiceResult.success(updatedItem.get(), 
                    String.format("Removed %d %s from %s", quantity, updatedItem.get().getUnit(), updatedItem.get().getName()));
            } else {
                return ServiceResult.error("Failed to retrieve updated item.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Adjust stock quantity (for corrections)
     */
    public ServiceResult<InventoryItem> adjustStock(int itemId, int newQuantity, String reason) {
        try {
            // Check if item exists
            Optional<InventoryItem> existingItem = inventoryDAO.getInventoryItemById(itemId);
            if (existingItem.isEmpty()) {
                return ServiceResult.error("Inventory item not found.");
            }
            
            if (newQuantity < 0) {
                return ServiceResult.error("New quantity cannot be negative.");
            }
            
            InventoryItem item = existingItem.get();
            int currentQuantity = item.getQuantity();
            int adjustment = newQuantity - currentQuantity;
            
            if (adjustment == 0) {
                return ServiceResult.success(item, "No adjustment needed - quantity is already correct.");
            }
            
            // Create transaction
            InventoryTransaction transaction = new InventoryTransaction(
                itemId,
                adjustment,
                InventoryTransaction.Reason.ADJUSTMENT,
                reason
            );
            
            inventoryDAO.createInventoryTransaction(transaction);
            
            // Get updated item
            Optional<InventoryItem> updatedItem = inventoryDAO.getInventoryItemById(itemId);
            if (updatedItem.isPresent()) {
                return ServiceResult.success(updatedItem.get(), 
                    String.format("Adjusted %s quantity from %d to %d", item.getName(), currentQuantity, newQuantity));
            } else {
                return ServiceResult.error("Failed to retrieve updated item.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get inventory item by ID
     */
    public ServiceResult<InventoryItem> getInventoryItemById(int itemId) {
        try {
            Optional<InventoryItem> item = inventoryDAO.getInventoryItemById(itemId);
            if (item.isPresent()) {
                return ServiceResult.success(item.get());
            } else {
                return ServiceResult.error("Inventory item not found.");
            }
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get all inventory items
     */
    public ServiceResult<List<InventoryItem>> getAllInventoryItems() {
        try {
            List<InventoryItem> items = inventoryDAO.getAllInventoryItems();
            return ServiceResult.success(items);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get inventory items by category
     */
    public ServiceResult<List<InventoryItem>> getInventoryItemsByCategory(Category category) {
        try {
            List<InventoryItem> items = inventoryDAO.getInventoryItemsByCategory(category);
            return ServiceResult.success(items);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Search inventory items
     */
    public ServiceResult<List<InventoryItem>> searchInventoryItems(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllInventoryItems();
            }
            
            List<InventoryItem> items = inventoryDAO.searchInventoryItems(searchTerm.trim());
            return ServiceResult.success(items);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get low stock items
     */
    public ServiceResult<List<InventoryItem>> getLowStockItems() {
        try {
            List<InventoryItem> items = inventoryDAO.getLowStockItems();
            return ServiceResult.success(items);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get expired items
     */
    public ServiceResult<List<InventoryItem>> getExpiredItems() {
        try {
            List<InventoryItem> items = inventoryDAO.getExpiredItems();
            return ServiceResult.success(items);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get items expiring soon
     */
    public ServiceResult<List<InventoryItem>> getItemsExpiringSoon() {
        try {
            List<InventoryItem> items = inventoryDAO.getItemsExpiringSoon();
            return ServiceResult.success(items);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Delete inventory item
     */
    public ServiceResult<Void> deleteInventoryItem(int itemId) {
        try {
            // Check if item exists
            Optional<InventoryItem> item = inventoryDAO.getInventoryItemById(itemId);
            if (item.isEmpty()) {
                return ServiceResult.error("Inventory item not found.");
            }
            
            boolean deleted = inventoryDAO.deleteInventoryItem(itemId);
            if (deleted) {
                return ServiceResult.success(null, "Inventory item deleted successfully.");
            } else {
                return ServiceResult.error("Failed to delete inventory item.");
            }
            
        } catch (SQLException e) {
            if (e.getMessage().contains("existing transactions")) {
                return ServiceResult.error("Cannot delete item with transaction history. Consider marking as inactive instead.");
            }
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get inventory statistics
     */
    public ServiceResult<InventoryStats> getInventoryStatistics() {
        try {
            InventoryDAO.InventoryStats daoStats = inventoryDAO.getInventoryStatistics();
            InventoryStats stats = new InventoryStats(
                daoStats.getTotalItems(),
                daoStats.getLowStockCount(),
                daoStats.getOutOfStockCount(),
                daoStats.getTotalValue()
            );
            return ServiceResult.success(stats);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get recent inventory transactions
     */
    public ServiceResult<List<InventoryTransaction>> getRecentTransactions(int limit) {
        try {
            List<InventoryTransaction> transactions = inventoryDAO.getRecentTransactions(limit);
            return ServiceResult.success(transactions);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get transactions for specific inventory item
     */
    public ServiceResult<List<InventoryTransaction>> getTransactionsByItem(int itemId) {
        try {
            List<InventoryTransaction> transactions = inventoryDAO.getTransactionsByInventoryId(itemId);
            return ServiceResult.success(transactions);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Validate inventory item data
     */
    private ValidationResult validateInventoryItemData(String name, Category category, 
                                                     int quantity, String unit, int threshold, 
                                                     BigDecimal costPerUnit, LocalDate expiryDate, Integer excludeId) {
        // Name validation
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.invalid("Item name is required.");
        }
        
        if (name.trim().length() < 2) {
            return ValidationResult.invalid("Item name must be at least 2 characters long.");
        }
        
        if (name.trim().length() > 100) {
            return ValidationResult.invalid("Item name must be less than 100 characters.");
        }
        
        // Category validation
        if (category == null) {
            return ValidationResult.invalid("Category is required.");
        }
        
        // Quantity validation
        if (quantity < 0) {
            return ValidationResult.invalid("Quantity cannot be negative.");
        }
        
        // Unit validation
        if (unit == null || unit.trim().isEmpty()) {
            return ValidationResult.invalid("Unit is required.");
        }
        
        // Threshold validation
        if (threshold < 0) {
            return ValidationResult.invalid("Threshold cannot be negative.");
        }
        
        // Cost validation
        if (costPerUnit != null && costPerUnit.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.invalid("Cost per unit cannot be negative.");
        }
        
        // Expiry date validation
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            return ValidationResult.invalid("Expiry date cannot be in the past.");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Service result wrapper class
     */
    public static class ServiceResult<T> {
        private final boolean success;
        private final T data;
        private final String message;
        
        private ServiceResult(boolean success, T data, String message) {
            this.success = success;
            this.data = data;
            this.message = message;
        }
        
        public static <T> ServiceResult<T> success(T data) {
            return new ServiceResult<>(true, data, null);
        }
        
        public static <T> ServiceResult<T> success(T data, String message) {
            return new ServiceResult<>(true, data, message);
        }
        
        public static <T> ServiceResult<T> error(String message) {
            return new ServiceResult<>(false, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public String getMessage() { return message; }
    }
    
    /**
     * Validation result class
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
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
} 