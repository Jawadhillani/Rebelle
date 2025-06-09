package com.rebelle.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * InventoryTransaction model class representing inventory changes
 */
public class InventoryTransaction {
    
    public enum TransactionType {
        ADD("Stock Added"),
        REMOVE("Stock Removed"),
        ADJUST("Stock Adjusted"),
        EXPIRED("Expired Items Removed"),
        DAMAGED("Damaged Items Removed");
        
        private final String displayName;
        
        TransactionType(String displayName) {
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
    
    public enum Reason {
        RESTOCK("Restock"),
        PURCHASE("Purchase"),
        RETURN("Return"),
        DAMAGE("Damage"),
        EXPIRY("Expiry"),
        USE("Use"),
        PATIENT_USE("Patient Use"),
        EXPIRED("Expired"),
        DAMAGED("Damaged"),
        LOST("Lost"),
        ADJUSTMENT("Adjustment"),
        OTHER("Other");
        
        private final String displayName;
        
        Reason(String displayName) {
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
    
    private int id;
    private int inventoryId;
    private TransactionType transactionType;
    private int quantityChange;
    private Reason reason;
    private Integer appointmentId;
    private LocalDateTime transactionDate;
    private String notes;
    
    // Related objects (loaded separately)
    private InventoryItem inventoryItem;
    private Appointment appointment;
    
    // Constructors
    public InventoryTransaction() {
        this.transactionDate = LocalDateTime.now();
    }
    
    public InventoryTransaction(int inventoryId, int quantityChange, Reason reason, String notes) {
        this();
        this.inventoryId = inventoryId;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.notes = notes;
        this.transactionType = quantityChange > 0 ? TransactionType.ADD : TransactionType.REMOVE;
    }
    
    public InventoryTransaction(int id, int inventoryId, TransactionType type, int quantityChange, 
                              Reason reason, Integer appointmentId, LocalDateTime transactionDate) {
        this.id = id;
        this.inventoryId = inventoryId;
        this.transactionType = type;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.appointmentId = appointmentId;
        this.transactionDate = transactionDate;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getInventoryId() {
        return inventoryId;
    }
    
    public void setInventoryId(int inventoryId) {
        this.inventoryId = inventoryId;
    }
    
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    public int getQuantityChange() {
        return quantityChange;
    }
    
    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
    }
    
    public Reason getReason() {
        return reason;
    }
    
    public void setReason(Reason reason) {
        this.reason = reason;
    }
    
    public Integer getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }
    
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }
    
    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }
    
    public Appointment getAppointment() {
        return appointment;
    }
    
    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
    
    // Utility Methods
    
    /**
     * Check if this is an addition transaction
     */
    public boolean isAddition() {
        return quantityChange > 0;
    }
    
    /**
     * Check if this is a removal transaction
     */
    public boolean isRemoval() {
        return quantityChange < 0;
    }
    
    /**
     * Get absolute quantity change
     */
    public int getAbsoluteQuantityChange() {
        return Math.abs(quantityChange);
    }
    
    /**
     * Get formatted transaction date
     */
    public String getFormattedDate() {
        if (transactionDate == null) {
            return "";
        }
        return transactionDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }
    
    /**
     * Get formatted transaction date (short)
     */
    public String getFormattedDateShort() {
        if (transactionDate == null) {
            return "";
        }
        return transactionDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }
    
    /**
     * Get item name (if inventory item is loaded)
     */
    public String getItemName() {
        return inventoryItem != null ? inventoryItem.getDisplayName() : "Unknown Item";
    }
    
    /**
     * Get patient name (if appointment and patient are loaded)
     */
    public String getPatientName() {
        if (appointment != null && appointment.getPatient() != null) {
            return appointment.getPatient().getDisplayName();
        }
        return null;
    }
    
    /**
     * Get quantity change with sign
     */
    public String getQuantityChangeDisplay() {
        if (quantityChange > 0) {
            return "+" + quantityChange;
        } else {
            return String.valueOf(quantityChange);
        }
    }
    
    /**
     * Get transaction description
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (isAddition()) {
            desc.append("Added ").append(quantityChange);
        } else {
            desc.append("Removed ").append(Math.abs(quantityChange));
        }
        
        if (inventoryItem != null) {
            desc.append(" ").append(inventoryItem.getUnit());
            desc.append(" of ").append(inventoryItem.getDisplayName());
        }
        
        if (reason != null) {
            desc.append(" (").append(reason.getDisplayName()).append(")");
        }
        
        return desc.toString();
    }
    
    /**
     * Get transaction summary for display
     */
    public String getSummary() {
        return String.format("%s: %s %s - %s", 
                           getFormattedDateShort(),
                           getQuantityChangeDisplay(),
                           getItemName(),
                           reason != null ? reason.getDisplayName() : "");
    }
    
    /**
     * Check if transaction is related to patient care
     */
    public boolean isPatientRelated() {
        return reason == Reason.USE || reason == Reason.PATIENT_USE;
    }
    
    /**
     * Check if transaction is today
     */
    public boolean isToday() {
        return transactionDate != null && 
               transactionDate.toLocalDate().equals(java.time.LocalDate.now());
    }
    
    /**
     * Get transaction color for UI based on type
     */
    public String getTransactionColor() {
        if (isAddition()) {
            return "green";
        } else {
            switch (reason) {
                case USE:
                case PATIENT_USE:
                case RESTOCK:
                    return "blue";
                case EXPIRY:
                case EXPIRED:
                case DAMAGE:
                case DAMAGED:
                    return "red";
                default:
                    return "orange";
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("InventoryTransaction{id=%d, item=%s, change=%+d, reason=%s, date=%s}", 
                           id, getItemName(), quantityChange, reason, getFormattedDateShort());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        InventoryTransaction transaction = (InventoryTransaction) obj;
        return id == transaction.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
} 