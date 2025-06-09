package com.rebelle.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expense - Represents a business expense for the practice
 */
public class Expense {
    private int id;
    private String description;
    private BigDecimal amount;
    private Category category;
    private PaymentMethod paymentMethod;
    private LocalDate expenseDate;
    private String vendor;
    private String receiptNumber;
    private String notes;
    private LocalDateTime createdAt;
    
    public enum Category {
        RENT("Rent"),
        UTILITIES("Utilities"),
        SUPPLIES("Supplies"),
        EQUIPMENT("Equipment"),
        MAINTENANCE("Maintenance"),
        MARKETING("Marketing"),
        INSURANCE("Insurance"),
        SALARY("Salary"),
        OTHER("Other");
        
        private final String displayName;
        
        Category(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
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
    
    // Constructor for new expense
    public Expense(String description, BigDecimal amount, Category category, 
                  PaymentMethod paymentMethod, LocalDate expenseDate, String vendor) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.expenseDate = expenseDate;
        this.vendor = vendor;
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor for loading from database
    public Expense(int id, String description, BigDecimal amount, Category category,
                  PaymentMethod paymentMethod, LocalDate expenseDate, String vendor,
                  String receiptNumber, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.expenseDate = expenseDate;
        this.vendor = vendor;
        this.receiptNumber = receiptNumber;
        this.notes = notes;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    
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
     * Get category display name
     */
    public String getCategoryDisplayName() {
        return category.getDisplayName();
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
            description, getFormattedAmount(), category.getDisplayName());
    }
} 