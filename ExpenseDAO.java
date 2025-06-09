package com.rebelle.dao;

import com.rebelle.models.Expense;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ExpenseDAO - Data Access Object for Business Expense operations
 */
public class ExpenseDAO {
    
    private final DatabaseManager dbManager;
    
    public ExpenseDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Create a new business expense
     */
    public Expense createExpense(Expense expense) throws SQLException {
        String sql = """
            INSERT INTO business_expenses (description, amount, category, payment_method, 
                                        expense_date, vendor, receipt_number, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, expense.getDescription());
            stmt.setBigDecimal(2, expense.getAmount());
            stmt.setString(3, expense.getCategory().name().toLowerCase());
            stmt.setString(4, expense.getPaymentMethod().name().toLowerCase());
            stmt.setString(5, expense.getExpenseDate().toString());
            stmt.setString(6, expense.getVendor());
            stmt.setString(7, expense.getReceiptNumber());
            stmt.setString(8, expense.getNotes());
            stmt.setString(9, expense.getCreatedAt().toString());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating expense failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    expense.setId(generatedKeys.getInt(1));
                    return expense;
                } else {
                    throw new SQLException("Creating expense failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Get expense by ID
     */
    public Optional<Expense> getExpenseById(int id) throws SQLException {
        String sql = "SELECT * FROM business_expenses WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToExpense(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all expenses
     */
    public List<Expense> getAllExpenses() throws SQLException {
        String sql = "SELECT * FROM business_expenses ORDER BY expense_date DESC, created_at DESC";
        List<Expense> expenses = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                expenses.add(mapResultSetToExpense(rs));
            }
        }
        
        return expenses;
    }
    
    /**
     * Get expenses by date range
     */
    public List<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
            SELECT * FROM business_expenses 
            WHERE expense_date BETWEEN ? AND ?
            ORDER BY expense_date DESC
            """;
        List<Expense> expenses = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapResultSetToExpense(rs));
                }
            }
        }
        
        return expenses;
    }
    
    /**
     * Get today's expenses
     */
    public List<Expense> getTodaysExpenses() throws SQLException {
        return getExpensesByDateRange(LocalDate.now(), LocalDate.now());
    }
    
    /**
     * Get this week's expenses
     */
    public List<Expense> getThisWeeksExpenses() throws SQLException {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return getExpensesByDateRange(startOfWeek, endOfWeek);
    }
    
    /**
     * Get this month's expenses
     */
    public List<Expense> getThisMonthsExpenses() throws SQLException {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        return getExpensesByDateRange(startOfMonth, endOfMonth);
    }
    
    /**
     * Update an existing expense
     */
    public boolean updateExpense(Expense expense) throws SQLException {
        String sql = """
            UPDATE business_expenses 
            SET description = ?, amount = ?, category = ?, payment_method = ?, 
                expense_date = ?, vendor = ?, receipt_number = ?, notes = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, expense.getDescription());
            stmt.setBigDecimal(2, expense.getAmount());
            stmt.setString(3, expense.getCategory().name().toLowerCase());
            stmt.setString(4, expense.getPaymentMethod().name().toLowerCase());
            stmt.setString(5, expense.getExpenseDate().toString());
            stmt.setString(6, expense.getVendor());
            stmt.setString(7, expense.getReceiptNumber());
            stmt.setString(8, expense.getNotes());
            stmt.setInt(9, expense.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete an expense
     */
    public boolean deleteExpense(int expenseId) throws SQLException {
        String sql = "DELETE FROM business_expenses WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, expenseId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Get expense statistics
     */
    public ExpenseStats getExpenseStatistics() throws SQLException {
        String sql = """
            SELECT 
                SUM(CASE WHEN expense_date = ? THEN amount ELSE 0 END) as today_total,
                SUM(CASE WHEN expense_date >= ? THEN amount ELSE 0 END) as week_total,
                SUM(CASE WHEN expense_date >= ? THEN amount ELSE 0 END) as month_total,
                COUNT(*) as total_expenses
            FROM business_expenses
            """;
        
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, now.toString());
            stmt.setString(2, startOfWeek.toString());
            stmt.setString(3, startOfMonth.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal todayTotal = rs.getBigDecimal("today_total");
                    BigDecimal weekTotal = rs.getBigDecimal("week_total");
                    BigDecimal monthTotal = rs.getBigDecimal("month_total");
                    int totalExpenses = rs.getInt("total_expenses");
                    
                    return new ExpenseStats(
                        todayTotal != null ? todayTotal : BigDecimal.ZERO,
                        weekTotal != null ? weekTotal : BigDecimal.ZERO,
                        monthTotal != null ? monthTotal : BigDecimal.ZERO,
                        totalExpenses
                    );
                }
            }
        }
        
        return new ExpenseStats(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
    
    /**
     * Get expense total for date range
     */
    public BigDecimal getExpenseTotal(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
            SELECT SUM(amount) as total
            FROM business_expenses 
            WHERE expense_date BETWEEN ? AND ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Search expenses
     */
    public List<Expense> searchExpenses(String searchTerm) throws SQLException {
        String sql = """
            SELECT * FROM business_expenses
            WHERE description LIKE ? OR vendor LIKE ? OR notes LIKE ?
            ORDER BY expense_date DESC
            """;
        
        List<Expense> expenses = new ArrayList<>();
        String searchPattern = "%" + searchTerm + "%";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapResultSetToExpense(rs));
                }
            }
        }
        
        return expenses;
    }
    
    /**
     * Get expenses by category
     */
    public List<Expense> getExpensesByCategory(Expense.Category category) throws SQLException {
        String sql = "SELECT * FROM business_expenses WHERE category = ? ORDER BY expense_date DESC";
        List<Expense> expenses = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.name().toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapResultSetToExpense(rs));
                }
            }
        }
        
        return expenses;
    }
    
    /**
     * Get category totals
     */
    public List<CategoryTotal> getCategoryTotals() throws SQLException {
        String sql = """
            SELECT category, SUM(amount) as total
            FROM business_expenses
            GROUP BY category
            ORDER BY total DESC
            """;
        
        List<CategoryTotal> totals = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String categoryStr = rs.getString("category");
                BigDecimal total = rs.getBigDecimal("total");
                
                if (categoryStr != null && total != null) {
                    Expense.Category category = Expense.Category.valueOf(categoryStr.toUpperCase());
                    totals.add(new CategoryTotal(category, total));
                }
            }
        }
        
        return totals;
    }
    
    /**
     * Map ResultSet to Expense object
     */
    private Expense mapResultSetToExpense(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String description = rs.getString("description");
        BigDecimal amount = rs.getBigDecimal("amount");
        Expense.Category category = Expense.Category.valueOf(rs.getString("category"));
        Expense.PaymentMethod paymentMethod = Expense.PaymentMethod.valueOf(rs.getString("payment_method"));
        LocalDate expenseDate = rs.getDate("expense_date").toLocalDate();
        String vendor = rs.getString("vendor");
        String receiptNumber = rs.getString("receipt_number");
        String notes = rs.getString("notes");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        
        return new Expense(id, description, amount, category, paymentMethod, expenseDate, vendor, receiptNumber, notes, createdAt);
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