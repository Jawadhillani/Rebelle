package com.rebelle.models;

/**
 * Represents the different categories of inventory items
 */
public enum Category {
    MEDICINE("Medicine"),
    SUPPLIES("Supplies"),
    EQUIPMENT("Equipment"),
    OTHER("Other");

    private final String displayName;

    Category(String displayName) {
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