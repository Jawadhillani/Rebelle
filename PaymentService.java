package com.rebelle.services;

import com.rebelle.dao.PaymentDAO;
import com.rebelle.dao.PatientDAO;
import com.rebelle.models.Payment;
import com.rebelle.models.Patient;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PaymentService - Business logic layer for patient payment operations
 */
public class PaymentService {
    
    private final PaymentDAO paymentDAO;
    private final PatientDAO patientDAO;
    
    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
        this.patientDAO = new PatientDAO();
    }
    
    /**
     * Record a new patient payment with validation
     */
    public ServiceResult<Payment> createPayment(int patientId, BigDecimal amount, Payment.PaymentMethod method, 
                                             LocalDate paymentDate, String description, String notes) {
        try {
            // Validate input
            ValidationResult validation = validatePaymentData(patientId, amount, method, paymentDate);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Create payment
            Payment payment = new Payment(
                0, // id will be set by database
                patientId,
                null, // patient will be loaded later
                amount,
                method,
                paymentDate,
                description,
                notes,
                LocalDateTime.now()
            );
            Payment createdPayment = paymentDAO.createPayment(payment);
            return ServiceResult.success(createdPayment, "Payment recorded successfully.");
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing payment
     */
    public ServiceResult<Boolean> updatePayment(int paymentId, int patientId, BigDecimal amount,
                                               Payment.PaymentMethod method, LocalDate paymentDate,
                                               String description, String notes) {
        try {
            // Check if payment exists
            Payment existingPayment = paymentDAO.getPaymentById(paymentId);
            if (existingPayment == null) {
                return ServiceResult.error("Payment not found.");
            }
            
            // Validate input
            ValidationResult validation = validatePaymentData(patientId, amount, method, paymentDate);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Update payment data
            existingPayment.setPatientId(patientId);
            existingPayment.setAmount(amount);
            existingPayment.setPaymentMethod(method);
            existingPayment.setPaymentDate(paymentDate);
            existingPayment.setDescription(description);
            existingPayment.setNotes(notes);
            
            Payment updatedPayment = paymentDAO.updatePayment(existingPayment);
            return ServiceResult.success(true, "Payment updated successfully.");
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get payment by ID
     */
    public ServiceResult<Payment> getPaymentById(int paymentId) {
        try {
            Payment payment = paymentDAO.getPaymentById(paymentId);
            if (payment != null) {
                return ServiceResult.success(payment);
            } else {
                return ServiceResult.error("Payment not found.");
            }
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get all payments
     */
    public ServiceResult<List<Payment>> getAllPayments() {
        try {
            List<Payment> payments = paymentDAO.getAllPayments();
            return ServiceResult.success(payments);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get payments for a specific patient
     */
    public ServiceResult<List<Payment>> getPaymentsByPatientId(int patientId) {
        try {
            List<Payment> payments = paymentDAO.getPaymentsByPatientId(patientId);
            return ServiceResult.success(payments);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get payments by date range
     */
    public ServiceResult<List<Payment>> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            List<Payment> payments = paymentDAO.getPaymentsByDateRange(startDate, endDate);
            return ServiceResult.success(payments);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get today's payments
     */
    public ServiceResult<List<Payment>> getTodaysPayments() {
        try {
            List<Payment> payments = paymentDAO.getPaymentsByDateRange(LocalDate.now(), LocalDate.now());
            return ServiceResult.success(payments);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get this week's payments
     */
    public ServiceResult<List<Payment>> getThisWeeksPayments() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
            List<Payment> payments = paymentDAO.getPaymentsByDateRange(startOfWeek, today);
            return ServiceResult.success(payments);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get this month's payments
     */
    public ServiceResult<List<Payment>> getThisMonthsPayments() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            List<Payment> payments = paymentDAO.getPaymentsByDateRange(startOfMonth, today);
            return ServiceResult.success(payments);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Search payments
     */
    public ServiceResult<List<Payment>> searchPayments(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllPayments();
            }
            
            List<Payment> payments = paymentDAO.searchPayments(searchTerm.trim());
            return ServiceResult.success(payments);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Delete a payment
     */
    public ServiceResult<Void> deletePayment(int paymentId) {
        try {
            // Check if payment exists
            Payment payment = paymentDAO.getPaymentById(paymentId);
            if (payment == null) {
                return ServiceResult.error("Payment not found.");
            }
            
            boolean deleted = paymentDAO.deletePayment(paymentId);
            if (deleted) {
                return ServiceResult.success(null, "Payment deleted successfully.");
            } else {
                return ServiceResult.error("Failed to delete payment.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get payment statistics
     */
    public ServiceResult<PaymentStats> getPaymentStatistics() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            PaymentDAO.PaymentStats daoStats = paymentDAO.getPaymentStatistics(startOfMonth, today);
            
            PaymentStats stats = new PaymentStats(
                daoStats.getTotalCount(),
                daoStats.getTotalAmount(),
                daoStats.getAverageAmount()
            );
            return ServiceResult.success(stats);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get payment total for date range
     */
    public ServiceResult<BigDecimal> getPaymentTotal(LocalDate startDate, LocalDate endDate) {
        try {
            BigDecimal total = paymentDAO.getPaymentTotal(startDate, endDate);
            return ServiceResult.success(total);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get patient payment history with total
     */
    public ServiceResult<PatientPaymentSummary> getPatientPaymentSummary(int patientId) {
        try {
            // Get patient info
            Optional<Patient> patient = patientDAO.getPatientById(patientId);
            if (patient.isEmpty()) {
                return ServiceResult.error("Patient not found.");
            }
            
            // Get payments
            List<Payment> payments = paymentDAO.getPaymentsByPatientId(patientId);
            
            // Calculate total
            BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            PatientPaymentSummary summary = new PatientPaymentSummary(
                patient.get(), payments, totalPaid
            );
            return ServiceResult.success(summary);
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Validate payment data
     */
    private ValidationResult validatePaymentData(int patientId, BigDecimal amount, 
                                               Payment.PaymentMethod paymentMethod, LocalDate paymentDate) {
        try {
            // Patient validation
            Optional<Patient> patient = patientDAO.getPatientById(patientId);
            if (patient.isEmpty()) {
                return ValidationResult.invalid("Patient not found.");
            }
            
            // Amount validation
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ValidationResult.invalid("Payment amount must be greater than 0.");
            }
            
            if (amount.compareTo(new BigDecimal("10000.00")) > 0) {
                return ValidationResult.invalid("Payment amount cannot exceed $10,000.00.");
            }
            
            // Payment method validation
            if (paymentMethod == null) {
                return ValidationResult.invalid("Payment method is required.");
            }
            
            // Date validation
            if (paymentDate == null) {
                return ValidationResult.invalid("Payment date is required.");
            }
            
            if (paymentDate.isAfter(LocalDate.now())) {
                return ValidationResult.invalid("Payment date cannot be in the future.");
            }
            
            if (paymentDate.isBefore(LocalDate.now().minusYears(1))) {
                return ValidationResult.invalid("Payment date cannot be more than 1 year ago.");
            }
            
            return ValidationResult.valid();
            
        } catch (SQLException e) {
            return ValidationResult.invalid("Database error during validation: " + e.getMessage());
        }
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
     * Patient payment summary class
     */
    public static class PatientPaymentSummary {
        private final Patient patient;
        private final List<Payment> payments;
        private final BigDecimal totalPaid;
        
        public PatientPaymentSummary(Patient patient, List<Payment> payments, BigDecimal totalPaid) {
            this.patient = patient;
            this.payments = payments;
            this.totalPaid = totalPaid;
        }
        
        public Patient getPatient() { return patient; }
        public List<Payment> getPayments() { return payments; }
        public BigDecimal getTotalPaid() { return totalPaid; }
    }
} 