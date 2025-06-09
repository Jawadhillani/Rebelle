package com.rebelle.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an inventory item in the medical practice
 */
public class InventoryItem {
    private int id;
    private String name;
    private Category category;
    private int quantity;
    private String unit;
    private int threshold;
    private BigDecimal costPerUnit;
    private String supplier;
    private LocalDate expiryDate;
    private String notes;
    private LocalDateTime updatedAt;

    // Default constructor
    public InventoryItem() {
        this.updatedAt = LocalDateTime.now();
        this.costPerUnit = BigDecimal.ZERO;
    }

    // Constructor for new items
    public InventoryItem(String name, Category category, int quantity, String unit, 
                        int threshold, BigDecimal costPerUnit, String supplier, 
                        LocalDate expiryDate, String notes) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.threshold = threshold;
        this.costPerUnit = costPerUnit;
        this.supplier = supplier;
        this.expiryDate = expiryDate;
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    // Full constructor for loading from database
    public InventoryItem(int id, String name, Category category, int quantity, 
                        String unit, int threshold, BigDecimal costPerUnit, String supplier, 
                        LocalDate expiryDate, String notes, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.threshold = threshold;
        this.costPerUnit = costPerUnit;
        this.supplier = supplier;
        this.expiryDate = expiryDate;
        this.notes = notes;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public boolean isLowStock() {
        return quantity <= threshold;
    }

    public boolean isOutOfStock() {
        return quantity <= 0;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon() {
        return expiryDate != null && 
               !expiryDate.isBefore(LocalDate.now()) && 
               expiryDate.isBefore(LocalDate.now().plusDays(30));
    }

    public BigDecimal getTotalValue() {
        return costPerUnit.multiply(BigDecimal.valueOf(quantity));
    }

    public Status getStatus() {
        if (isOutOfStock()) {
            return Status.OUT_OF_STOCK;
        } else if (isExpired()) {
            return Status.EXPIRED;
        } else if (isExpiringSoon()) {
            return Status.EXPIRING_SOON;
        } else if (isLowStock()) {
            return Status.LOW_STOCK;
        } else {
            return Status.IN_STOCK;
        }
    }

    public String getDisplayName() {
        return name;
    }

    public String getQuantityWithUnit() {
        return quantity + " " + unit;
    }

    // Override methods
    @Override
    public String toString() {
        return name + " (" + quantity + " " + unit + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 