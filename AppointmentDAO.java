package com.rebelle.dao;

import com.rebelle.models.Appointment;
import com.rebelle.models.Patient;
import com.rebelle.models.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Appointment operations
 */
public class AppointmentDAO {
    
    private final DatabaseManager dbManager;
    
    public AppointmentDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Create a new appointment
     */
    public Appointment createAppointment(Appointment appointment) {
        String sql = "INSERT INTO appointments (patient_id, service_id, appointment_date, start_time, " +
                    "duration_minutes, status, notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, appointment.getPatientId());
            stmt.setObject(2, appointment.getServiceId());
            stmt.setDate(3, Date.valueOf(appointment.getAppointmentDate()));
            stmt.setTime(4, Time.valueOf(appointment.getAppointmentTime()));
            stmt.setInt(5, appointment.getDurationMinutes());
            stmt.setString(6, appointment.getStatus().name());
            stmt.setString(7, appointment.getNotes());
            stmt.setTimestamp(8, Timestamp.valueOf(appointment.getCreatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return null;
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    appointment.setId(generatedKeys.getInt(1));
                    return appointment;
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Update an existing appointment
     */
    public boolean updateAppointment(Appointment appointment) {
        String sql = "UPDATE appointments SET patient_id = ?, service_id = ?, appointment_date = ?, " +
                    "start_time = ?, duration_minutes = ?, status = ?, notes = ?, updated_at = ? " +
                    "WHERE id = ?";
                    
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, appointment.getPatientId());
            stmt.setObject(2, appointment.getServiceId());
            stmt.setDate(3, Date.valueOf(appointment.getAppointmentDate()));
            stmt.setTime(4, Time.valueOf(appointment.getAppointmentTime()));
            stmt.setInt(5, appointment.getDurationMinutes());
            stmt.setString(6, appointment.getStatus().name());
            stmt.setString(7, appointment.getNotes());
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(9, appointment.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete an appointment
     */
    public boolean deleteAppointment(int id) {
        String sql = "DELETE FROM appointments WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Find appointment by ID
     */
    public Optional<Appointment> getAppointmentById(int id) {
        String sql = "SELECT a.*, p.name as patient_name, s.name as service_name " +
                    "FROM appointments a " +
                    "LEFT JOIN patients p ON a.patient_id = p.id " +
                    "LEFT JOIN services s ON a.service_id = s.id " +
                    "WHERE a.id = ?";
                    
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAppointment(rs));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Optional.empty();
    }
    
    /**
     * Find appointments by date
     */
    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        String sql = "SELECT a.*, p.name as patient_name, s.name as service_name " +
                    "FROM appointments a " +
                    "LEFT JOIN patients p ON a.patient_id = p.id " +
                    "LEFT JOIN services s ON a.service_id = s.id " +
                    "WHERE a.appointment_date = ? " +
                    "ORDER BY a.start_time";
                    
        List<Appointment> appointments = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    /**
     * Find appointments by patient
     */
    public List<Appointment> getAppointmentsByPatient(int patientId) {
        String sql = "SELECT a.*, p.name as patient_name, s.name as service_name " +
                    "FROM appointments a " +
                    "LEFT JOIN patients p ON a.patient_id = p.id " +
                    "LEFT JOIN services s ON a.service_id = s.id " +
                    "WHERE a.patient_id = ? " +
                    "ORDER BY a.appointment_date DESC, a.start_time";
                    
        List<Appointment> appointments = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, patientId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    /**
     * Find appointments by date range
     */
    public List<Appointment> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT a.*, p.name as patient_name, s.name as service_name " +
                    "FROM appointments a " +
                    "LEFT JOIN patients p ON a.patient_id = p.id " +
                    "LEFT JOIN services s ON a.service_id = s.id " +
                    "WHERE a.appointment_date BETWEEN ? AND ? " +
                    "ORDER BY a.appointment_date, a.start_time";
                    
        List<Appointment> appointments = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    /**
     * Get all appointments
     */
    public List<Appointment> getAllAppointments() {
        String sql = "SELECT a.*, p.name as patient_name, s.name as service_name " +
                    "FROM appointments a " +
                    "LEFT JOIN patients p ON a.patient_id = p.id " +
                    "LEFT JOIN services s ON a.service_id = s.id " +
                    "ORDER BY a.appointment_date DESC, a.start_time";
                    
        List<Appointment> appointments = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                appointments.add(mapResultSetToAppointment(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    /**
     * Get today's appointments
     */
    public List<Appointment> getTodaysAppointments() {
        return getAppointmentsByDate(LocalDate.now());
    }
    
    /**
     * Get today's appointment count
     */
    public int getTodaysAppointmentCount() {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Get upcoming appointment count
     */
    public int getUpcomingAppointmentCount() {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date > ? AND status = 'SCHEDULED'";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Find conflicting appointments
     */
    public List<Appointment> findConflictingAppointments(LocalDate date, LocalTime startTime, 
                                                       int durationMinutes, Integer excludeAppointmentId) {
        String sql = "SELECT a.*, p.name as patient_name, s.name as service_name " +
                    "FROM appointments a " +
                    "LEFT JOIN patients p ON a.patient_id = p.id " +
                    "LEFT JOIN services s ON a.service_id = s.id " +
                    "WHERE a.appointment_date = ? " +
                    "AND a.status != 'CANCELLED' " +
                    "AND ((a.start_time <= ? AND a.start_time + INTERVAL '1 minute' * a.duration_minutes > ?) " +
                    "OR (a.start_time < ? + INTERVAL '1 minute' * ? AND a.start_time >= ?))";
                    
        if (excludeAppointmentId != null) {
            sql += " AND a.id != ?";
        }
        
        List<Appointment> appointments = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            LocalTime endTime = startTime.plusMinutes(durationMinutes);
            
            stmt.setDate(1, Date.valueOf(date));
            stmt.setTime(2, Time.valueOf(endTime));
            stmt.setTime(3, Time.valueOf(startTime));
            stmt.setTime(4, Time.valueOf(startTime));
            stmt.setInt(5, durationMinutes);
            stmt.setTime(6, Time.valueOf(startTime));
            
            if (excludeAppointmentId != null) {
                stmt.setInt(7, excludeAppointmentId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapResultSetToAppointment(rs));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    /**
     * Get appointment statistics
     */
    public AppointmentStats getStats(LocalDate date) {
        String sql = "SELECT " +
                    "COUNT(*) as total, " +
                    "SUM(CASE WHEN status = 'SCHEDULED' THEN 1 ELSE 0 END) as scheduled, " +
                    "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
                    "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled " +
                    "FROM appointments " +
                    "WHERE appointment_date = ?";
                    
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AppointmentStats(
                        rs.getInt("total"),
                        rs.getInt("scheduled"),
                        rs.getInt("completed"),
                        rs.getInt("cancelled")
                    );
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return new AppointmentStats(0, 0, 0, 0);
    }
    
    /**
     * Map ResultSet to Appointment object
     */
    private Appointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getInt("id"));
        appointment.setPatientId(rs.getInt("patient_id"));
        appointment.setServiceId(rs.getInt("service_id"));
        appointment.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        appointment.setAppointmentTime(rs.getTime("start_time").toLocalTime());
        appointment.setDurationMinutes(rs.getInt("duration_minutes"));
        appointment.setStatus(Appointment.Status.valueOf(rs.getString("status")));
        appointment.setNotes(rs.getString("notes"));
        appointment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            appointment.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return appointment;
    }
    
    /**
     * Appointment statistics class
     */
    public static class AppointmentStats {
        private final int total;
        private final int scheduled;
        private final int completed;
        private final int cancelled;
        
        public AppointmentStats(int total, int scheduled, int completed, int cancelled) {
            this.total = total;
            this.scheduled = scheduled;
            this.completed = completed;
            this.cancelled = cancelled;
        }
        
        public int getTotal() { return total; }
        public int getScheduled() { return scheduled; }
        public int getCompleted() { return completed; }
        public int getCancelled() { return cancelled; }
    }
} 