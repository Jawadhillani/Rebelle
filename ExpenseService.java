package com.rebelle.services;

import com.rebelle.dao.ExpenseDAO;
import com.rebelle.models.Expense;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ExpenseService - Business logic layer for business expense operations
 */
public class ExpenseService {
    
    private final ExpenseDAO expenseDAO;
    
    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
    }
    
    /**
     * Record a new business expense with validation
     */
    public ServiceResult<Expense> recordExpense(String description, BigDecimal amount, Expense.Category category,
                                              Expense.PaymentMethod paymentMethod, 
                                              LocalDate expenseDate, String vendor,
                                              String receiptNumber, String notes) {
        try {
            // Validate input
            ValidationResult validation = validateExpenseData(description, amount, category, 
                                                           paymentMethod, expenseDate, vendor);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Create expense
            Expense expense = new Expense(description, amount, category, paymentMethod, expenseDate, vendor);
            expense.setReceiptNumber(receiptNumber);
            expense.setNotes(notes);
            
            Expense createdExpense = expenseDAO.createExpense(expense);
            return ServiceResult.success(createdExpense, 
                String.format("Expense of %s recorded for %s", 
                            createdExpense.getFormattedAmount(), 
                            createdExpense.getDescription()));
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing expense
     */
    public ServiceResult<Expense> updateExpense(int expenseId, String description, BigDecimal amount,
                                              Expense.Category category,
                                              Expense.PaymentMethod paymentMethod, 
                                              LocalDate expenseDate, String vendor,
                                              String receiptNumber, String notes) {
        try {
            // Check if expense exists
            Optional<Expense> existingExpense = expenseDAO.getExpenseById(expenseId);
            if (existingExpense.isEmpty()) {
                return ServiceResult.error("Expense not found.");
            }
            
            // Validate input
            ValidationResult validation = validateExpenseData(description, amount, category, 
                                                           paymentMethod, expenseDate, vendor);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Update expense data
            Expense expense = existingExpense.get();
            expense.setDescription(description);
            expense.setAmount(amount);
            expense.setCategory(category);
            expense.setPaymentMethod(paymentMethod);
            expense.setExpenseDate(expenseDate);
            expense.setVendor(vendor);
            expense.setReceiptNumber(receiptNumber);
            expense.setNotes(notes);
            
            boolean updated = expenseDAO.updateExpense(expense);
            if (updated) {
                // Reload expense
                Optional<Expense> updatedExpense = expenseDAO.getExpenseById(expenseId);
                return ServiceResult.success(updatedExpense.get(), "Expense updated successfully.");
            } else {
                return ServiceResult.error("Failed to update expense.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get expense by ID
     */
    public ServiceResult<Expense> getExpenseById(int expenseId) {
        try {
            Optional<Expense> expense = expenseDAO.getExpenseById(expenseId);
            if (expense.isPresent()) {
                return ServiceResult.success(expense.get());
            } else {
                return ServiceResult.error("Expense not found.");
            }
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get all expenses
     */
    public ServiceResult<List<Expense>> getAllExpenses() {
        try {
            List<Expense> expenses = expenseDAO.getAllExpenses();
            return ServiceResult.success(expenses);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get expenses by date range
     */
    public ServiceResult<List<Expense>> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            List<Expense> expenses = expenseDAO.getExpensesByDateRange(startDate, endDate);
            return ServiceResult.success(expenses);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get today's expenses
     */
    public ServiceResult<List<Expense>> getTodaysExpenses() {
        try {
            List<Expense> expenses = expenseDAO.getTodaysExpenses();
            return ServiceResult.success(expenses);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get this week's expenses
     */
    public ServiceResult<List<Expense>> getThisWeeksExpenses() {
        try {
            List<Expense> expenses = expenseDAO.getThisWeeksExpenses();
            return ServiceResult.success(expenses);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get this month's expenses
     */
    public ServiceResult<List<Expense>> getThisMonthsExpenses() {
        try {
            List<Expense> expenses = expenseDAO.getThisMonthsExpenses();
            return ServiceResult.success(expenses);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Search expenses
     */
    public ServiceResult<List<Expense>> searchExpenses(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllExpenses();
            }
            
            List<Expense> expenses = expenseDAO.searchExpenses(searchTerm.trim());
            return ServiceResult.success(expenses);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Delete an expense
     */
    public ServiceResult<Void> deleteExpense(int expenseId) {
        try {
            // Check if expense exists
            Optional<Expense> expense = expenseDAO.getExpenseById(expenseId);
            if (expense.isEmpty()) {
                return ServiceResult.error("Expense not found.");
            }
            
            boolean deleted = expenseDAO.deleteExpense(expenseId);
            if (deleted) {
                return ServiceResult.success(null, "Expense deleted successfully.");
            } else {
                return ServiceResult.error("Failed to delete expense.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get expense statistics
     */
    public ServiceResult<ExpenseStats> getExpenseStatistics() {
        try {
            ExpenseDAO.ExpenseStats daoStats = expenseDAO.getExpenseStatistics();
            ExpenseStats stats = new ExpenseStats(
                daoStats.getTodayTotal(),
                daoStats.getWeekTotal(),
                daoStats.getMonthTotal(),
                daoStats.getTotalExpenses()
            );
            return ServiceResult.success(stats);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get expense total for date range
     */
    public ServiceResult<BigDecimal> getExpenseTotal(LocalDate startDate, LocalDate endDate) {
        try {
            BigDecimal total = expenseDAO.getExpenseTotal(startDate, endDate);
            return ServiceResult.success(total);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get expenses by category
     */
    public ServiceResult<List<Expense>> getExpensesByCategory(Expense.Category category) {
        try {
            List<Expense> expenses = expenseDAO.getExpensesByCategory(category);
            return ServiceResult.success(expenses);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get category totals
     */
    public ServiceResult<List<CategoryTotal>> getCategoryTotals() {
        try {
            List<ExpenseDAO.CategoryTotal> totals = expenseDAO.getCategoryTotals();
            List<CategoryTotal> serviceTotals = totals.stream()
                .map(t -> new CategoryTotal(t.getCategory(), t.getTotal()))
                .toList();
            return ServiceResult.success(serviceTotals);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Validate expense data
     */
    private ValidationResult validateExpenseData(String description, BigDecimal amount, Expense.Category category,
                                               Expense.PaymentMethod paymentMethod, 
                                               LocalDate expenseDate, String vendor) {
        // Description validation
        if (description == null || description.trim().isEmpty()) {
            return ValidationResult.invalid("Expense description is required.");
        }
        
        if (description.length() > 200) {
            return ValidationResult.invalid("Expense description cannot exceed 200 characters.");
        }
        
        // Amount validation
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Expense amount must be greater than 0.");
        }
        
        if (amount.compareTo(new BigDecimal("100000.00")) > 0) {
            return ValidationResult.invalid("Expense amount cannot exceed $100,000.00.");
        }
        
        // Category validation
        if (category == null) {
            return ValidationResult.invalid("Expense category is required.");
        }
        
        // Payment method validation
        if (paymentMethod == null) {
            return ValidationResult.invalid("Payment method is required.");
        }
        
        // Date validation
        if (expenseDate == null) {
            return ValidationResult.invalid("Expense date is required.");
        }
        
        if (expenseDate.isAfter(LocalDate.now())) {
            return ValidationResult.invalid("Expense date cannot be in the future.");
        }
        
        if (expenseDate.isBefore(LocalDate.now().minusYears(1))) {
            return ValidationResult.invalid("Expense date cannot be more than 1 year ago.");
        }
        
        // Vendor validation
        if (vendor == null || vendor.trim().isEmpty()) {
            return ValidationResult.invalid("Vendor name is required.");
        }
        
        if (vendor.length() > 100) {
            return ValidationResult.invalid("Vendor name cannot exceed 100 characters.");
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
     * Expense statistics class
     */
    public static class ExpenseStats {
        private final BigDecimal todayTotal;
        private final BigDecimal weekTotal;
        private final BigDecimal monthTotal;
        private final int totalExpenses;
        
        public ExpenseStats(BigDecimal todayTotal, BigDecimal weekTotal, BigDecimal monthTotal, int totalExpenses) {
            this.todayTotal = todayTotal;
            this.weekTotal = weekTotal;
            this.monthTotal = monthTotal;
            this.totalExpenses = totalExpenses;
        }
        
        public BigDecimal getTodayTotal() { return todayTotal; }
        public BigDecimal getWeekTotal() { return weekTotal; }
        public BigDecimal getMonthTotal() { return monthTotal; }
        public int getTotalExpenses() { return totalExpenses; }
    }
    
    /**
     * Category total class
     */
    public static class CategoryTotal {
        private final Expense.Category category;
        private final BigDecimal total;
        
        public CategoryTotal(Expense.Category category, BigDecimal total) {
            this.category = category;
            this.total = total;
        }
        
        public Expense.Category getCategory() { return category; }
        public BigDecimal getTotal() { return total; }
        public String getCategoryDisplay() { return category.getDisplayName(); }
        public String getFormattedTotal() { return String.format("$%.2f", total); }
    }
} 