package com.rebelle.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service model class representing a medical service or procedure
 */
public class Service {
    
    private int id;
    private String name;
    private String description;
    private BigDecimal defaultPrice;
    private int durationMinutes;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Service() {
        this.defaultPrice = BigDecimal.ZERO;
        this.durationMinutes = 30;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Service(String name, String description, BigDecimal defaultPrice, int durationMinutes) {
        this();
        this.name = name;
        this.description = description;
        this.defaultPrice = defaultPrice;
        this.durationMinutes = durationMinutes;
    }
    
    public Service(int id, String name, String description, BigDecimal defaultPrice, 
                   int durationMinutes, boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultPrice = defaultPrice;
        this.durationMinutes = durationMinutes;
        this.isActive = isActive;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }
    
    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }
    
    public int getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Utility Methods
    
    /**
     * Get display name for UI
     */
    public String getDisplayName() {
        if (name == null || name.trim().isEmpty()) {
            return "Unnamed Service";
        }
        return name.trim();
    }
    
    /**
     * Get formatted price string
     */
    public String getFormattedPrice() {
        if (defaultPrice == null) {
            return "$0.00";
        }
        return String.format("$%.2f", defaultPrice);
    }
    
    /**
     * Get duration as formatted string
     */
    public String getDurationString() {
        if (durationMinutes < 60) {
            return durationMinutes + " min";
        } else {
            int hours = durationMinutes / 60;
            int mins = durationMinutes % 60;
            if (mins == 0) {
                return hours + (hours == 1 ? " hour" : " hours");
            } else {
                return hours + "h " + mins + "m";
            }
        }
    }
    
    /**
     * Get service summary for display
     */
    public String getSummary() {
        return String.format("%s (%s, %s)", getDisplayName(), getDurationString(), getFormattedPrice());
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Service service = (Service) obj;
        return id == service.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
} 