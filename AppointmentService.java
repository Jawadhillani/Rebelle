package com.rebelle.services;

import com.rebelle.dao.AppointmentDAO;
import com.rebelle.dao.PatientDAO;
import com.rebelle.dao.ServiceDAO;
import com.rebelle.models.Appointment;
import com.rebelle.models.Patient;
import com.rebelle.models.Service;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * AppointmentService - Business logic layer for appointment operations
 */
public class AppointmentService {
    
    private final AppointmentDAO appointmentDAO;
    private final PatientDAO patientDAO;
    private final ServiceDAO serviceDAO;
    
    public AppointmentService() {
        this.appointmentDAO = new AppointmentDAO();
        this.patientDAO = new PatientDAO();
        this.serviceDAO = new ServiceDAO();
    }
    
    /**
     * Create a new appointment with validation
     */
    public ServiceResult<Appointment> createAppointment(int patientId, Integer serviceId, 
                                                      LocalDate appointmentDate, LocalTime appointmentTime, 
                                                      Integer durationMinutes, String notes) {
        // Validate input
        ValidationResult validation = validateAppointmentData(patientId, serviceId, appointmentDate, 
                                                            appointmentTime, durationMinutes, null);
        if (!validation.isValid()) {
            return ServiceResult.error(validation.getErrorMessage());
        }
        
        // Set default duration if not provided
        int duration = durationMinutes != null ? durationMinutes : 30;
        
        // If service is provided, use service's default duration
        if (serviceId != null) {
            Optional<Service> service = serviceDAO.getServiceById(serviceId);
            if (service.isPresent()) {
                duration = service.get().getDurationMinutes();
            }
        }
        
        // Check for conflicts
        List<Appointment> conflicts = appointmentDAO.findConflictingAppointments(
            appointmentDate, appointmentTime, duration, null);
        
        if (!conflicts.isEmpty()) {
            return ServiceResult.error(
                String.format("Appointment conflicts with existing appointment at %s", 
                            conflicts.get(0).getFormattedTime()));
        }
        
        // Create appointment
        Appointment appointment = new Appointment(patientId, appointmentDate, appointmentTime);
        appointment.setServiceId(serviceId);
        appointment.setDurationMinutes(duration);
        appointment.setNotes(notes);
        appointment.setStatus(Appointment.Status.SCHEDULED);
        
        Appointment createdAppointment = appointmentDAO.createAppointment(appointment);
        if (createdAppointment != null) {
            return ServiceResult.success(createdAppointment, "Appointment scheduled successfully.");
        } else {
            return ServiceResult.error("Failed to create appointment.");
        }
    }
    
    /**
     * Update an existing appointment
     */
    public ServiceResult<Appointment> updateAppointment(int appointmentId, int patientId, Integer serviceId, 
                                                       LocalDate appointmentDate, LocalTime appointmentTime, 
                                                       Integer durationMinutes, Appointment.Status status, String notes) {
        // Check if appointment exists
        Optional<Appointment> existingAppointment = appointmentDAO.getAppointmentById(appointmentId);
        if (existingAppointment.isEmpty()) {
            return ServiceResult.error("Appointment not found.");
        }
        
        // Validate input
        ValidationResult validation = validateAppointmentData(patientId, serviceId, appointmentDate, 
                                                            appointmentTime, durationMinutes, appointmentId);
        if (!validation.isValid()) {
            return ServiceResult.error(validation.getErrorMessage());
        }
        
        // Set default duration if not provided
        int duration = durationMinutes != null ? durationMinutes : 30;
        
        // If service is provided, use service's default duration
        if (serviceId != null) {
            Optional<Service> service = serviceDAO.getServiceById(serviceId);
            if (service.isPresent()) {
                duration = service.get().getDurationMinutes();
            }
        }
        
        // Check for conflicts (excluding current appointment)
        List<Appointment> conflicts = appointmentDAO.findConflictingAppointments(
            appointmentDate, appointmentTime, duration, appointmentId);
        
        if (!conflicts.isEmpty()) {
            return ServiceResult.error(
                String.format("Appointment conflicts with existing appointment at %s", 
                            conflicts.get(0).getFormattedTime()));
        }
        
        // Update appointment data
        Appointment appointment = existingAppointment.get();
        appointment.setPatientId(patientId);
        appointment.setServiceId(serviceId);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setDurationMinutes(duration);
        appointment.setStatus(status != null ? status : Appointment.Status.SCHEDULED);
        appointment.setNotes(notes);
        
        boolean updated = appointmentDAO.updateAppointment(appointment);
        if (updated) {
            // Reload appointment with related objects
            Optional<Appointment> updatedAppointment = appointmentDAO.getAppointmentById(appointmentId);
            return ServiceResult.success(updatedAppointment.get(), "Appointment updated successfully.");
        } else {
            return ServiceResult.error("Failed to update appointment.");
        }
    }
    
    /**
     * Get appointment by ID
     */
    public ServiceResult<Appointment> getAppointmentById(int appointmentId) {
        Optional<Appointment> appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment.isPresent()) {
            return ServiceResult.success(appointment.get());
        } else {
            return ServiceResult.error("Appointment not found.");
        }
    }
    
    /**
     * Get all appointments
     */
    public ServiceResult<List<Appointment>> getAllAppointments() {
        List<Appointment> appointments = appointmentDAO.getAllAppointments();
        return ServiceResult.success(appointments);
    }
    
    /**
     * Get appointments for today
     */
    public ServiceResult<List<Appointment>> getTodaysAppointments() {
        List<Appointment> appointments = appointmentDAO.getTodaysAppointments();
        return ServiceResult.success(appointments);
    }
    
    /**
     * Get appointments by date
     */
    public ServiceResult<List<Appointment>> getAppointmentsByDate(LocalDate date) {
        List<Appointment> appointments = appointmentDAO.getAppointmentsByDate(date);
        return ServiceResult.success(appointments);
    }
    
    /**
     * Get appointments for a patient
     */
    public ServiceResult<List<Appointment>> getAppointmentsByPatient(int patientId) {
        List<Appointment> appointments = appointmentDAO.getAppointmentsByPatient(patientId);
        return ServiceResult.success(appointments);
    }
    
    /**
     * Get appointments within date range
     */
    public ServiceResult<List<Appointment>> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Appointment> appointments = appointmentDAO.getAppointmentsByDateRange(startDate, endDate);
        return ServiceResult.success(appointments);
    }
    
    /**
     * Cancel an appointment
     */
    public ServiceResult<Appointment> cancelAppointment(int appointmentId, String reason) {
        Optional<Appointment> appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment.isEmpty()) {
            return ServiceResult.error("Appointment not found.");
        }
        
        Appointment appt = appointment.get();
        appt.setStatus(Appointment.Status.CANCELLED);
        appt.setNotes(reason);
        
        boolean updated = appointmentDAO.updateAppointment(appt);
        if (updated) {
            return ServiceResult.success(appt, "Appointment cancelled successfully.");
        } else {
            return ServiceResult.error("Failed to cancel appointment.");
        }
    }
    
    /**
     * Complete an appointment
     */
    public ServiceResult<Appointment> completeAppointment(int appointmentId, String notes) {
        Optional<Appointment> appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment.isEmpty()) {
            return ServiceResult.error("Appointment not found.");
        }
        
        Appointment appt = appointment.get();
        appt.setStatus(Appointment.Status.COMPLETED);
        appt.setNotes(notes);
        
        boolean updated = appointmentDAO.updateAppointment(appt);
        if (updated) {
            return ServiceResult.success(appt, "Appointment marked as completed.");
        } else {
            return ServiceResult.error("Failed to complete appointment.");
        }
    }
    
    /**
     * Delete an appointment
     */
    public ServiceResult<Void> deleteAppointment(int appointmentId) {
        Optional<Appointment> appointment = appointmentDAO.getAppointmentById(appointmentId);
        if (appointment.isEmpty()) {
            return ServiceResult.error("Appointment not found.");
        }
        
        boolean deleted = appointmentDAO.deleteAppointment(appointmentId);
        if (deleted) {
            return ServiceResult.success(null, "Appointment deleted successfully.");
        } else {
            return ServiceResult.error("Failed to delete appointment.");
        }
    }
    
    /**
     * Get appointment statistics
     */
    public ServiceResult<AppointmentStats> getAppointmentStatistics() {
        int todaysCount = appointmentDAO.getTodaysAppointmentCount();
        int upcomingCount = appointmentDAO.getUpcomingAppointmentCount();
        
        AppointmentStats stats = new AppointmentStats(todaysCount, upcomingCount);
        return ServiceResult.success(stats);
    }
    
    /**
     * Get available services
     */
    public ServiceResult<List<Service>> getAvailableServices() {
        List<Service> services = serviceDAO.getAllActiveServices();
        return ServiceResult.success(services);
    }
    
    /**
     * Validate appointment data
     */
    private ValidationResult validateAppointmentData(int patientId, Integer serviceId, 
                                                   LocalDate appointmentDate, LocalTime appointmentTime, 
                                                   Integer durationMinutes, Integer excludeAppointmentId) {
        try {
            // Patient validation
            Optional<Patient> patient = patientDAO.getPatientById(patientId);
            if (patient.isEmpty()) {
                return ValidationResult.invalid("Patient not found.");
            }
            
            // Service validation (if provided)
            if (serviceId != null) {
                Optional<Service> service = serviceDAO.getServiceById(serviceId);
                if (service.isEmpty()) {
                    return ValidationResult.invalid("Service not found.");
                }
                if (!service.get().isActive()) {
                    return ValidationResult.invalid("Selected service is not active.");
                }
            }
            
            // Date validation
            if (appointmentDate == null) {
                return ValidationResult.invalid("Appointment date is required.");
            }
            
            if (appointmentDate.isBefore(LocalDate.now())) {
                return ValidationResult.invalid("Appointment date cannot be in the past.");
            }
            
            // Time validation
            if (appointmentTime == null) {
                return ValidationResult.invalid("Appointment time is required.");
            }
            
            // Check if appointment is in the past (for today's appointments)
            if (appointmentDate.equals(LocalDate.now()) && appointmentTime.isBefore(LocalTime.now())) {
                return ValidationResult.invalid("Appointment time cannot be in the past.");
            }
            
            // Duration validation
            if (durationMinutes != null && (durationMinutes < 5 || durationMinutes > 480)) {
                return ValidationResult.invalid("Duration must be between 5 minutes and 8 hours.");
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
     * Appointment statistics class
     */
    public static class AppointmentStats {
        private final int todaysAppointments;
        private final int upcomingAppointments;
        
        public AppointmentStats(int todaysAppointments, int upcomingAppointments) {
            this.todaysAppointments = todaysAppointments;
            this.upcomingAppointments = upcomingAppointments;
        }
        
        public int getTodaysAppointments() { return todaysAppointments; }
        public int getUpcomingAppointments() { return upcomingAppointments; }
    }
} 