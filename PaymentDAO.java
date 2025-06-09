package com.rebelle.dao;

import com.rebelle.models.Payment;
import com.rebelle.models.Patient;
import com.rebelle.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * PaymentDAO - Data Access Object for handling patient payment operations
 */
public class PaymentDAO {
    private final DatabaseConnection dbConnection;
    private final PatientDAO patientDAO;
    
    public PaymentDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.patientDAO = new PatientDAO();
    }
    
    /**
     * Create a new payment record
     */
    public Payment createPayment(Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (patient_id, amount, payment_method, payment_date, " +
                    "description, notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, payment.getPatientId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setString(3, payment.getPaymentMethod().name());
            stmt.setDate(4, Date.valueOf(payment.getPaymentDate()));
            stmt.setString(5, payment.getDescription());
            stmt.setString(6, payment.getNotes());
            stmt.setTimestamp(7, Timestamp.valueOf(payment.getCreatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payment failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    payment.setId(generatedKeys.getInt(1));
                    return payment;
                } else {
                    throw new SQLException("Creating payment failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Get payment by ID
     */
    public Payment getPaymentById(int paymentId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, paymentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayment(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Get all payments
     */
    public List<Payment> getAllPayments() throws SQLException {
        String sql = "SELECT * FROM payments ORDER BY payment_date DESC";
        List<Payment> payments = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                payments.add(mapResultSetToPayment(rs));
            }
        }
        return payments;
    }
    
    /**
     * Get payments by date range
     */
    public List<Payment> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE payment_date BETWEEN ? AND ? ORDER BY payment_date DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapResultSetToPayment(rs));
                }
            }
        }
        return payments;
    }
    
    /**
     * Get payments by patient ID
     */
    public List<Payment> getPaymentsByPatientId(int patientId) throws SQLException {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE patient_id = ? ORDER BY payment_date DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapResultSetToPayment(rs));
                }
            }
        }
        return payments;
    }
    
    /**
     * Update payment record
     */
    public Payment updatePayment(Payment payment) throws SQLException {
        String sql = "UPDATE payments SET patient_id = ?, amount = ?, payment_method = ?, " +
                    "payment_date = ?, description = ?, notes = ? WHERE id = ?";
                    
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, payment.getPatientId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setString(3, payment.getPaymentMethod().name());
            stmt.setDate(4, Date.valueOf(payment.getPaymentDate()));
            stmt.setString(5, payment.getDescription());
            stmt.setString(6, payment.getNotes());
            stmt.setInt(7, payment.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating payment failed, no rows affected.");
            }
            
            return payment;
        }
    }
    
    /**
     * Delete payment record
     */
    public boolean deletePayment(int id) throws SQLException {
        String sql = "DELETE FROM payments WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }
    
    /**
     * Search payments by description or notes
     */
    public List<Payment> searchPayments(String query) throws SQLException {
        String sql = "SELECT * FROM payments WHERE description LIKE ? OR notes LIKE ? " +
                    "ORDER BY payment_date DESC";
        List<Payment> payments = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapResultSetToPayment(rs));
                }
            }
        }
        return payments;
    }
    
    /**
     * Get payment statistics
     */
    public PaymentStats getPaymentStatistics(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT COUNT(*) as total_count, SUM(amount) as total_amount, " +
                    "AVG(amount) as average_amount FROM payments WHERE payment_date BETWEEN ? AND ?";
                    
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PaymentStats(
                        rs.getInt("total_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("average_amount")
                    );
                }
            }
        }
        return new PaymentStats(0, null, null);
    }
    
    /**
     * Get payment total for a date range
     */
    public BigDecimal getPaymentTotal(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT SUM(amount) as total FROM payments WHERE payment_date BETWEEN ? AND ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get payment method totals
     */
    public List<PaymentMethodTotal> getPaymentMethodTotals(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT payment_method, COUNT(*) as count, SUM(amount) as total " +
                    "FROM payments WHERE payment_date BETWEEN ? AND ? " +
                    "GROUP BY payment_method ORDER BY total DESC";
        List<PaymentMethodTotal> totals = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    totals.add(new PaymentMethodTotal(
                        Payment.PaymentMethod.valueOf(rs.getString("payment_method")),
                        rs.getInt("count"),
                        rs.getBigDecimal("total")
                    ));
                }
            }
        }
        return totals;
    }
    
    /**
     * Map ResultSet to Payment object
     */
    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment(
            rs.getInt("id"),
            rs.getInt("patient_id"),
            patientDAO.getPatientById(rs.getInt("patient_id")).orElse(null),
            rs.getBigDecimal("amount"),
            Payment.PaymentMethod.valueOf(rs.getString("payment_method")),
            rs.getDate("payment_date").toLocalDate(),
            rs.getString("description"),
            rs.getString("notes"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
        return payment;
    }
    
    /**
     * Payment statistics class
     */
    public static class PaymentStats {
        private final int totalCount;
        private final BigDecimal totalAmount;
        private final BigDecimal averageAmount;
        
        public PaymentStats(int totalCount, BigDecimal totalAmount, BigDecimal averageAmount) {
            this.totalCount = totalCount;
            this.totalAmount = totalAmount;
            this.averageAmount = averageAmount;
        }
        
        public int getTotalCount() { return totalCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getAverageAmount() { return averageAmount; }
    }
    
    /**
     * Payment method total class
     */
    public static class PaymentMethodTotal {
        private final Payment.PaymentMethod method;
        private final int count;
        private final BigDecimal total;
        
        public PaymentMethodTotal(Payment.PaymentMethod method, int count, BigDecimal total) {
            this.method = method;
            this.count = count;
            this.total = total;
        }
        
        public Payment.PaymentMethod getMethod() { return method; }
        public int getCount() { return count; }
        public BigDecimal getTotal() { return total; }
        public String getMethodDisplayName() { return method.getDisplayName(); }
    }
} 