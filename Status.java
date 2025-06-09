package com.rebelle.models;

/**
 * Represents the different statuses of inventory items
 */
public enum Status {
    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
    EXPIRED("Expired"),
    EXPIRING_SOON("Expiring Soon");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
} 