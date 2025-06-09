package com.rebelle.services;

import com.rebelle.dao.PatientDAO;
import com.rebelle.models.Patient;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PatientService - Business logic layer for patient operations
 */
public class PatientService {
    
    private final PatientDAO patientDAO;
    
    public PatientService() {
        this.patientDAO = new PatientDAO();
    }
    
    /**
     * Create a new patient with validation
     */
    public ServiceResult<Patient> createPatient(String name, String phone, String email, 
                                              String address, LocalDate dateOfBirth, String medicalNotes) {
        try {
            // Validate input
            ValidationResult validation = validatePatientData(name, phone, email, dateOfBirth, null);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Check for duplicates
            if (patientDAO.patientExists(phone, email, null)) {
                return ServiceResult.error("A patient with this phone number or email already exists.");
            }
            
            // Create patient
            Patient patient = new Patient(name.trim(), 
                                        phone != null ? phone.trim() : null,
                                        email != null ? email.trim() : null,
                                        address != null ? address.trim() : null,
                                        dateOfBirth);
            patient.setMedicalNotes(medicalNotes);
            
            Patient createdPatient = patientDAO.createPatient(patient);
            return ServiceResult.success(createdPatient, "Patient created successfully.");
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing patient
     */
    public ServiceResult<Patient> updatePatient(int patientId, String name, String phone, String email, 
                                              String address, LocalDate dateOfBirth, String medicalNotes) {
        try {
            // Check if patient exists
            Optional<Patient> existingPatient = patientDAO.getPatientById(patientId);
            if (existingPatient.isEmpty()) {
                return ServiceResult.error("Patient not found.");
            }
            
            // Validate input
            ValidationResult validation = validatePatientData(name, phone, email, dateOfBirth, patientId);
            if (!validation.isValid()) {
                return ServiceResult.error(validation.getErrorMessage());
            }
            
            // Check for duplicates (excluding current patient)
            if (patientDAO.patientExists(phone, email, patientId)) {
                return ServiceResult.error("Another patient with this phone number or email already exists.");
            }
            
            // Update patient data
            Patient patient = existingPatient.get();
            patient.setName(name.trim());
            patient.setPhone(phone != null ? phone.trim() : null);
            patient.setEmail(email != null ? email.trim() : null);
            patient.setAddress(address != null ? address.trim() : null);
            patient.setDateOfBirth(dateOfBirth);
            patient.setMedicalNotes(medicalNotes);
            
            boolean updated = patientDAO.updatePatient(patient);
            if (updated) {
                return ServiceResult.success(patient, "Patient updated successfully.");
            } else {
                return ServiceResult.error("Failed to update patient.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get patient by ID
     */
    public ServiceResult<Patient> getPatientById(int patientId) {
        try {
            Optional<Patient> patient = patientDAO.getPatientById(patientId);
            if (patient.isPresent()) {
                return ServiceResult.success(patient.get());
            } else {
                return ServiceResult.error("Patient not found.");
            }
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get all patients
     */
    public ServiceResult<List<Patient>> getAllPatients() {
        try {
            List<Patient> patients = patientDAO.getAllPatients();
            return ServiceResult.success(patients);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Search patients
     */
    public ServiceResult<List<Patient>> searchPatients(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllPatients();
            }
            
            List<Patient> patients = patientDAO.searchPatients(searchTerm.trim());
            return ServiceResult.success(patients);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Delete patient
     */
    public ServiceResult<Void> deletePatient(int patientId) {
        try {
            // Check if patient exists
            Optional<Patient> patient = patientDAO.getPatientById(patientId);
            if (patient.isEmpty()) {
                return ServiceResult.error("Patient not found.");
            }
            
            boolean deleted = patientDAO.deletePatient(patientId);
            if (deleted) {
                return ServiceResult.success(null, "Patient deleted successfully.");
            } else {
                return ServiceResult.error("Failed to delete patient.");
            }
            
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get patient statistics
     */
    public ServiceResult<PatientStats> getPatientStatistics() {
        try {
            int totalPatients = patientDAO.getPatientCount();
            List<Patient> recentPatients = patientDAO.getRecentPatients(5);
            
            PatientStats stats = new PatientStats(totalPatients, recentPatients.size());
            return ServiceResult.success(stats);
        } catch (SQLException e) {
            return ServiceResult.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Validate patient data
     */
    private ValidationResult validatePatientData(String name, String phone, String email, 
                                               LocalDate dateOfBirth, Integer excludeId) {
        // Name validation
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.invalid("Patient name is required.");
        }
        
        if (name.trim().length() < 2) {
            return ValidationResult.invalid("Patient name must be at least 2 characters long.");
        }
        
        if (name.trim().length() > 100) {
            return ValidationResult.invalid("Patient name must be less than 100 characters.");
        }
        
        // Phone validation (if provided)
        if (phone != null && !phone.trim().isEmpty()) {
            String cleanPhone = phone.replaceAll("[^0-9+\\-\\s()]", "");
            if (cleanPhone.length() < 7) {
                return ValidationResult.invalid("Phone number must be at least 7 digits.");
            }
        }
        
        // Email validation (if provided)
        if (email != null && !email.trim().isEmpty()) {
            if (!isValidEmail(email.trim())) {
                return ValidationResult.invalid("Please enter a valid email address.");
            }
        }
        
        // Date of birth validation (if provided)
        if (dateOfBirth != null) {
            if (dateOfBirth.isAfter(LocalDate.now())) {
                return ValidationResult.invalid("Date of birth cannot be in the future.");
            }
            
            if (dateOfBirth.isBefore(LocalDate.now().minusYears(150))) {
                return ValidationResult.invalid("Date of birth cannot be more than 150 years ago.");
            }
        }
        
        // At least one contact method required
        if ((phone == null || phone.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
            return ValidationResult.invalid("Please provide at least a phone number or email address.");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
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
     * Patient statistics class
     */
    public static class PatientStats {
        private final int totalPatients;
        private final int recentPatients;
        
        public PatientStats(int totalPatients, int recentPatients) {
            this.totalPatients = totalPatients;
            this.recentPatients = recentPatients;
        }
        
        public int getTotalPatients() { return totalPatients; }
        public int getRecentPatients() { return recentPatients; }
    }
} 