package com.rebelle.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Period;

/**
 * DateTimeUtils - Utility class for date and time operations
 */
public class DateTimeUtils {
    
    // Common date formatters
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    public static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    
    /**
     * Format LocalDate to display string
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMATTER);
    }
    
    /**
     * Format LocalDateTime to display string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * Format LocalDate to short string
     */
    public static String formatDateShort(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(SHORT_DATE_FORMATTER);
    }
    
    /**
     * Calculate age from date of birth
     */
    public static int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
    
    /**
     * Get age as formatted string
     */
    public static String getAgeString(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return "Age unknown";
        }
        int age = calculateAge(dateOfBirth);
        return age + (age == 1 ? " year old" : " years old");
    }
    
    /**
     * Parse date string safely
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Parse datetime string safely
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Check if a date is today
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }
    
    /**
     * Check if a date is in the past
     */
    public static boolean isPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }
    
    /**
     * Check if a date is in the future
     */
    public static boolean isFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }
    
    /**
     * Get relative time string (e.g., "2 days ago", "in 3 days")
     */
    public static String getRelativeTimeString(LocalDate date) {
        if (date == null) {
            return "";
        }
        
        LocalDate today = LocalDate.now();
        long daysDiff = date.toEpochDay() - today.toEpochDay();
        
        if (daysDiff == 0) {
            return "Today";
        } else if (daysDiff == 1) {
            return "Tomorrow";
        } else if (daysDiff == -1) {
            return "Yesterday";
        } else if (daysDiff > 0) {
            return "In " + daysDiff + " days";
        } else {
            return Math.abs(daysDiff) + " days ago";
        }
    }
    
    /**
     * Get current timestamp for database
     */
    public static String getCurrentTimestamp() {
        return LocalDateTime.now().toString();
    }
    
    /**
     * Get current date string for database
     */
    public static String getCurrentDateString() {
        return LocalDate.now().toString();
    }
    
    /**
     * Format a LocalTime object to a 12-hour time string (e.g., "9:30 AM")
     */
    public static String formatTime(LocalTime time) {
        return time.format(TIME_FORMATTER);
    }
    
    /**
     * Parse a time string in 12-hour format to LocalTime
     */
    public static LocalTime parseTime(String timeStr) {
        return LocalTime.parse(timeStr, TIME_FORMATTER);
    }
    
    /**
     * Check if a time is within business hours (8 AM to 6 PM)
     */
    public static boolean isWithinBusinessHours(LocalTime time) {
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }
    
    /**
     * Check if a date is a weekday (Monday to Friday)
     */
    public static boolean isWeekday(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return dayOfWeek >= 1 && dayOfWeek <= 5;
    }
    
    /**
     * Get the next business day (skips weekends)
     */
    public static LocalDate getNextBusinessDay(LocalDate date) {
        LocalDate nextDay = date.plusDays(1);
        while (!isWeekday(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }
    
    /**
     * Get the previous business day (skips weekends)
     */
    public static LocalDate getPreviousBusinessDay(LocalDate date) {
        LocalDate prevDay = date.minusDays(1);
        while (!isWeekday(prevDay)) {
            prevDay = prevDay.minusDays(1);
        }
        return prevDay;
    }
} 