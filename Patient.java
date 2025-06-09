package com.rebelle.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patient model class representing a patient in the medical practice
 */
public class Patient {
    
    private int id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private LocalDate dateOfBirth;
    private String medicalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Patient() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Patient(String name, String phone, String email, String address, LocalDate dateOfBirth) {
        this();
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }
    
    public Patient(int id, String name, String phone, String email, String address, 
                   LocalDate dateOfBirth, String medicalNotes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.medicalNotes = medicalNotes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        updateTimestamp();
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
        updateTimestamp();
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
        updateTimestamp();
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
        updateTimestamp();
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        updateTimestamp();
    }
    
    public String getMedicalNotes() {
        return medicalNotes;
    }
    
    public void setMedicalNotes(String medicalNotes) {
        this.medicalNotes = medicalNotes;
        updateTimestamp();
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
    
    // Utility Methods
    
    /**
     * Calculate patient's age based on date of birth
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }
    
    /**
     * Get patient's age as a formatted string
     */
    public String getAgeString() {
        int age = getAge();
        return age > 0 ? age + " years old" : "Age unknown";
    }
    
    /**
     * Update the updated timestamp
     */
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if patient has complete contact information
     */
    public boolean hasCompleteContact() {
        return (phone != null && !phone.trim().isEmpty()) || 
               (email != null && !email.trim().isEmpty());
    }
    
    /**
     * Get display name for UI
     */
    public String getDisplayName() {
        if (name == null || name.trim().isEmpty()) {
            return "Unnamed Patient";
        }
        return name.trim();
    }
    
    /**
     * Get formatted contact information
     */
    public String getContactInfo() {
        StringBuilder contact = new StringBuilder();
        
        if (phone != null && !phone.trim().isEmpty()) {
            contact.append("📞 ").append(phone);
        }
        
        if (email != null && !email.trim().isEmpty()) {
            if (contact.length() > 0) {
                contact.append(" | ");
            }
            contact.append("✉️ ").append(email);
        }
        
        return contact.length() > 0 ? contact.toString() : "No contact info";
    }
    
    /**
     * Get the patient's full name
     * @return The patient's full name
     */
    public String getFullName() {
        return name;
    }
    
    /**
     * Check if the patient has contact information
     * @return true if the patient has either phone or email
     */
    public boolean hasContactInfo() {
        return (phone != null && !phone.trim().isEmpty()) || 
               (email != null && !email.trim().isEmpty());
    }
    
    /**
     * Get the patient's primary contact method
     * @return The patient's phone number if available, otherwise email
     */
    public String getPrimaryContact() {
        if (phone != null && !phone.trim().isEmpty()) {
            return phone;
        }
        return email;
    }
    
    @Override
    public String toString() {
        return String.format("Patient{id=%d, name='%s', phone='%s', email='%s', age=%d}", 
                           id, name, phone, email, getAge());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Patient patient = (Patient) obj;
        return id == patient.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
} 