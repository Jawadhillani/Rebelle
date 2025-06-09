package com.rebelle.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment - Represents a patient payment for services
 */
public class Payment {
    private int id;
    private int patientId;
    private Patient patient;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String description;
    private String notes;
    private LocalDateTime createdAt;
    
    public enum PaymentMethod {
        CASH("Cash"),
        CHECK("Check"),
        CREDIT_CARD("Credit Card"),
        DEBIT_CARD("Debit Card"),
        BANK_TRANSFER("Bank Transfer"),
        OTHER("Other");
        
        private final String displayName;
        
        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Constructor for new payment
    public Payment(int patientId, BigDecimal amount, PaymentMethod paymentMethod, 
                  LocalDate paymentDate, String description) {
        this.patientId = patientId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor for loading from database
    public Payment(int id, int patientId, Patient patient, BigDecimal amount,
                  PaymentMethod paymentMethod, LocalDate paymentDate, String description,
                  String notes, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.patient = patient;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.description = description;
        this.notes = notes;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * Get formatted amount with currency symbol
     */
    public String getFormattedAmount() {
        return String.format("$%.2f", amount);
    }
    
    /**
     * Get payment method display name
     */
    public String getPaymentMethodDisplayName() {
        return paymentMethod.getDisplayName();
    }
    
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", 
            description, getFormattedAmount(), paymentMethod.getDisplayName());
    }
} 