package com.rebelle.utils;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * ValidationUtils - Utility class for form validation
 */
public class ValidationUtils {
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    // Phone validation pattern (allows various formats)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]?[0-9]{7,15}$"
    );
    
    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validate phone number
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        // Remove all non-digit characters except +
        String cleanPhone = phone.replaceAll("[^0-9+]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
    
    /**
     * Validate required text field
     */
    public static boolean isValidRequiredText(String text, int minLength) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return text.trim().length() >= minLength;
    }
    
    /**
     * Validate text length
     */
    public static boolean isValidTextLength(String text, int maxLength) {
        if (text == null) {
            return true; // null is valid for optional fields
        }
        return text.length() <= maxLength;
    }
    
    /**
     * Validate date of birth
     */
    public static boolean isValidDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return true; // Optional field
        }
        
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(150);
        
        return !dateOfBirth.isAfter(today) && !dateOfBirth.isBefore(minDate);
    }
    
    /**
     * Validate that at least one contact method is provided
     */
    public static boolean hasValidContact(String phone, String email) {
        return isValidPhone(phone) || isValidEmail(email);
    }
    
    /**
     * Clean and format phone number
     */
    public static String formatPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = phone.replaceAll("[^0-9+]", "");
        
        // Basic formatting for US numbers
        if (cleaned.length() == 10 && !cleaned.startsWith("+")) {
            return String.format("(%s) %s-%s", 
                                cleaned.substring(0, 3),
                                cleaned.substring(3, 6),
                                cleaned.substring(6, 10));
        }
        
        return cleaned;
    }
    
    /**
     * Validate and clean email
     */
    public static String cleanEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
    
    /**
     * Validate name format
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = name.trim();
        
        // Check length
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            return false;
        }
        
        // Check for valid characters (letters, spaces, hyphens, apostrophes)
        return trimmed.matches("^[a-zA-Z\\s\\-']+$");
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Comprehensive patient validation
     */
    public static ValidationResult validatePatient(String name, String phone, String email, 
                                                 String address, LocalDate dateOfBirth) {
        
        // Name validation
        if (!isValidName(name)) {
            return ValidationResult.invalid("Please enter a valid name (2-100 characters, letters only)");
        }
        
        // Contact validation
        if (!hasValidContact(phone, email)) {
            return ValidationResult.invalid("Please provide at least a valid phone number or email address");
        }
        
        // Phone validation (if provided)
        if (phone != null && !phone.trim().isEmpty() && !isValidPhone(phone)) {
            return ValidationResult.invalid("Please enter a valid phone number");
        }
        
        // Email validation (if provided)
        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            return ValidationResult.invalid("Please enter a valid email address");
        }
        
        // Date of birth validation
        if (!isValidDateOfBirth(dateOfBirth)) {
            return ValidationResult.invalid("Please enter a valid date of birth (not in future, not more than 150 years ago)");
        }
        
        // Address length validation
        if (!isValidTextLength(address, 500)) {
            return ValidationResult.invalid("Address must be less than 500 characters");
        }
        
        return ValidationResult.valid();
    }
} 