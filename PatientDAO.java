package com.rebelle.dao;

import com.rebelle.models.Patient;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PatientDAO - Data Access Object for Patient operations
 */
public class PatientDAO {
    
    private final DatabaseManager dbManager;
    
    public PatientDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Create a new patient
     */
    public Patient createPatient(Patient patient) throws SQLException {
        String sql = """
            INSERT INTO patients (name, phone, email, address, date_of_birth, medical_notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getPhone());
            stmt.setString(3, patient.getEmail());
            stmt.setString(4, patient.getAddress());
            stmt.setString(5, patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null);
            stmt.setString(6, patient.getMedicalNotes());
            stmt.setString(7, patient.getCreatedAt().toString());
            stmt.setString(8, patient.getUpdatedAt().toString());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating patient failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    patient.setId(generatedKeys.getInt(1));
                    return patient;
                } else {
                    throw new SQLException("Creating patient failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Get patient by ID
     */
    public Optional<Patient> getPatientById(int id) throws SQLException {
        String sql = "SELECT * FROM patients WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPatient(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all patients
     */
    public List<Patient> getAllPatients() throws SQLException {
        String sql = "SELECT * FROM patients ORDER BY name ASC";
        List<Patient> patients = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                patients.add(mapResultSetToPatient(rs));
            }
        }
        
        return patients;
    }
    
    /**
     * Search patients by name, phone, or email
     */
    public List<Patient> searchPatients(String searchTerm) throws SQLException {
        String sql = """
            SELECT * FROM patients 
            WHERE name LIKE ? OR phone LIKE ? OR email LIKE ?
            ORDER BY name ASC
            """;
        
        List<Patient> patients = new ArrayList<>();
        String searchPattern = "%" + searchTerm + "%";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapResultSetToPatient(rs));
                }
            }
        }
        
        return patients;
    }
    
    /**
     * Update an existing patient
     */
    public boolean updatePatient(Patient patient) throws SQLException {
        String sql = """
            UPDATE patients 
            SET name = ?, phone = ?, email = ?, address = ?, date_of_birth = ?, medical_notes = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getPhone());
            stmt.setString(3, patient.getEmail());
            stmt.setString(4, patient.getAddress());
            stmt.setString(5, patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null);
            stmt.setString(6, patient.getMedicalNotes());
            stmt.setString(7, LocalDateTime.now().toString());
            stmt.setInt(8, patient.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete a patient
     */
    public boolean deletePatient(int patientId) throws SQLException {
        // Check if patient has any appointments first
        if (hasAppointments(patientId)) {
            throw new SQLException("Cannot delete patient with existing appointments. Please cancel all appointments first.");
        }
        
        String sql = "DELETE FROM patients WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, patientId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Get patient count
     */
    public int getPatientCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM patients";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return 0;
    }
    
    /**
     * Get recently added patients (last 30 days)
     */
    public List<Patient> getRecentPatients(int limit) throws SQLException {
        String sql = """
            SELECT * FROM patients 
            WHERE created_at >= date('now', '-30 days')
            ORDER BY created_at DESC
            LIMIT ?
            """;
        
        List<Patient> patients = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapResultSetToPatient(rs));
                }
            }
        }
        
        return patients;
    }
    
    /**
     * Check if patient exists by phone or email
     */
    public boolean patientExists(String phone, String email, Integer excludeId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM patients WHERE ");
        List<String> conditions = new ArrayList<>();
        
        if (phone != null && !phone.trim().isEmpty()) {
            conditions.add("phone = ?");
        }
        if (email != null && !email.trim().isEmpty()) {
            conditions.add("email = ?");
        }
        
        if (conditions.isEmpty()) {
            return false;
        }
        
        sql.append(String.join(" OR ", conditions));
        
        if (excludeId != null) {
            sql.append(" AND id != ?");
        }
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            
            if (phone != null && !phone.trim().isEmpty()) {
                stmt.setString(paramIndex++, phone.trim());
            }
            if (email != null && !email.trim().isEmpty()) {
                stmt.setString(paramIndex++, email.trim());
            }
            if (excludeId != null) {
                stmt.setInt(paramIndex, excludeId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if patient has appointments
     */
    private boolean hasAppointments(int patientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE patient_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, patientId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to Patient object
     */
    private Patient mapResultSetToPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        
        patient.setId(rs.getInt("id"));
        patient.setName(rs.getString("name"));
        patient.setPhone(rs.getString("phone"));
        patient.setEmail(rs.getString("email"));
        patient.setAddress(rs.getString("address"));
        
        String dobString = rs.getString("date_of_birth");
        if (dobString != null && !dobString.isEmpty()) {
            patient.setDateOfBirth(LocalDate.parse(dobString));
        }
        
        patient.setMedicalNotes(rs.getString("medical_notes"));
        
        String createdAtString = rs.getString("created_at");
        if (createdAtString != null) {
            patient.setCreatedAt(LocalDateTime.parse(createdAtString));
        }
        
        String updatedAtString = rs.getString("updated_at");
        if (updatedAtString != null) {
            patient.setUpdatedAt(LocalDateTime.parse(updatedAtString));
        }
        
        return patient;
    }
} 