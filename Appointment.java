package com.rebelle.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Appointment model class representing a medical appointment
 */
public class Appointment {
    
    public enum Status {
        SCHEDULED("Scheduled"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled"),
        NO_SHOW("No Show");
        
        private final String displayName;
        
        Status(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private int id;
    private int patientId;
    private Integer serviceId; // Optional - can be null
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private int durationMinutes;
    private Status status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Related objects (loaded separately)
    private Patient patient;
    private Service service;
    
    // Constructors
    public Appointment() {
        this.status = Status.SCHEDULED;
        this.durationMinutes = 30;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Appointment(int patientId, LocalDate appointmentDate, LocalTime appointmentTime) {
        this();
        this.patientId = patientId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
    }
    
    public Appointment(int id, int patientId, Integer serviceId, LocalDate appointmentDate, 
                      LocalTime appointmentTime, int durationMinutes, Status status, 
                      String notes, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.serviceId = serviceId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getPatientId() {
        return patientId;
    }
    
    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }
    
    public Integer getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }
    
    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }
    
    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
    
    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }
    
    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
    
    public int getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Patient getPatient() {
        return patient;
    }
    
    public void setPatient(Patient patient) {
        this.patient = patient;
    }
    
    public Service getService() {
        return service;
    }
    
    public void setService(Service service) {
        this.service = service;
    }
    
    // Utility Methods
    
    /**
     * Get appointment date and time as LocalDateTime
     */
    public LocalDateTime getAppointmentDateTime() {
        if (appointmentDate == null || appointmentTime == null) {
            return null;
        }
        return LocalDateTime.of(appointmentDate, appointmentTime);
    }
    
    /**
     * Set appointment date and time from LocalDateTime
     */
    public void setAppointmentDateTime(LocalDateTime dateTime) {
        if (dateTime != null) {
            this.appointmentDate = dateTime.toLocalDate();
            this.appointmentTime = dateTime.toLocalTime();
        }
    }
    
    /**
     * Get end time of appointment
     */
    public LocalTime getEndTime() {
        if (appointmentTime == null) {
            return null;
        }
        return appointmentTime.plusMinutes(durationMinutes);
    }
    
    /**
     * Get end date and time as LocalDateTime
     */
    public LocalDateTime getEndDateTime() {
        LocalDateTime startDateTime = getAppointmentDateTime();
        if (startDateTime == null) {
            return null;
        }
        return startDateTime.plusMinutes(durationMinutes);
    }
    
    /**
     * Check if appointment is today
     */
    public boolean isToday() {
        return appointmentDate != null && appointmentDate.equals(LocalDate.now());
    }
    
    /**
     * Check if appointment is in the past
     */
    public boolean isPast() {
        LocalDateTime appointmentDateTime = getAppointmentDateTime();
        return appointmentDateTime != null && appointmentDateTime.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if appointment is upcoming (future)
     */
    public boolean isUpcoming() {
        LocalDateTime appointmentDateTime = getAppointmentDateTime();
        return appointmentDateTime != null && appointmentDateTime.isAfter(LocalDateTime.now());
    }
    
    /**
     * Get formatted date string
     */
    public String getFormattedDate() {
        if (appointmentDate == null) {
            return "";
        }
        return appointmentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
    
    /**
     * Get formatted time string
     */
    public String getFormattedTime() {
        if (appointmentTime == null) {
            return "";
        }
        return appointmentTime.format(DateTimeFormatter.ofPattern("h:mm a"));
    }
    
    /**
     * Get formatted date and time string
     */
    public String getFormattedDateTime() {
        return getFormattedDate() + " at " + getFormattedTime();
    }
    
    /**
     * Get duration as formatted string
     */
    public String getDurationString() {
        if (durationMinutes < 60) {
            return durationMinutes + " min";
        } else {
            int hours = durationMinutes / 60;
            int mins = durationMinutes % 60;
            if (mins == 0) {
                return hours + (hours == 1 ? " hour" : " hours");
            } else {
                return hours + "h " + mins + "m";
            }
        }
    }
    
    /**
     * Get patient name (if patient object is loaded)
     */
    public String getPatientName() {
        return patient != null ? patient.getDisplayName() : "Unknown Patient";
    }
    
    /**
     * Get service name (if service object is loaded)
     */
    public String getServiceName() {
        return service != null ? service.getDisplayName() : "General Consultation";
    }
    
    /**
     * Get appointment summary for display
     */
    public String getSummary() {
        return String.format("%s - %s (%s)", 
                           getPatientName(), 
                           getFormattedDateTime(), 
                           getDurationString());
    }
    
    /**
     * Check if appointment conflicts with another appointment
     */
    public boolean conflictsWith(Appointment other) {
        if (other == null || !this.appointmentDate.equals(other.appointmentDate)) {
            return false;
        }
        
        LocalTime thisStart = this.appointmentTime;
        LocalTime thisEnd = this.getEndTime();
        LocalTime otherStart = other.appointmentTime;
        LocalTime otherEnd = other.getEndTime();
        
        // Check if times overlap
        return thisStart.isBefore(otherEnd) && otherStart.isBefore(thisEnd);
    }
    
    @Override
    public String toString() {
        return String.format("Appointment{id=%d, patient=%s, date=%s, time=%s, status=%s}", 
                           id, getPatientName(), getFormattedDate(), getFormattedTime(), status);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Appointment appointment = (Appointment) obj;
        return id == appointment.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
} 