package com.rebelle.utils;

import com.rebelle.models.Appointment;
import com.rebelle.models.Patient;
import com.rebelle.services.AppointmentService;
import com.rebelle.services.PatientService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TestDataGenerator - Utility class to generate sample data for testing
 */
public class TestDataGenerator {
    
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final Random random;
    
    public TestDataGenerator() {
        this.patientService = new PatientService();
        this.appointmentService = new AppointmentService();
        this.random = new Random();
    }
    
    /**
     * Generate sample patients and appointments for testing
     */
    public void generateSampleData() {
        System.out.println("Generating sample data...");
        
        try {
            // Generate sample patients
            List<Patient> samplePatients = generateSamplePatients();
            
            // Generate sample appointments
            generateSampleAppointments(samplePatients);
            
            System.out.println("Sample data generated successfully!");
            
        } catch (Exception e) {
            System.err.println("Error generating sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate sample patients
     */
    private List<Patient> generateSamplePatients() {
        List<Patient> createdPatients = new ArrayList<>();
        
        String[][] sampleData = {
            {"John Smith", "555-0101", "john.smith@email.com", "123 Oak Street, Springfield", "1985-03-15"},
            {"Sarah Johnson", "555-0102", "sarah.j@email.com", "456 Pine Ave, Springfield", "1990-07-22"},
            {"Michael Brown", "555-0103", "m.brown@email.com", "789 Elm Road, Springfield", "1978-11-03"},
            {"Emily Davis", "555-0104", "emily.davis@email.com", "321 Maple Dr, Springfield", "1995-01-18"},
            {"Robert Wilson", "555-0105", "rob.wilson@email.com", "654 Cedar Lane, Springfield", "1982-09-27"},
            {"Lisa Anderson", "555-0106", "lisa.a@email.com", "987 Birch Street, Springfield", "1988-04-11"},
            {"David Taylor", "555-0107", "", "147 Walnut Ave, Springfield", "1975-12-08"},
            {"Jennifer Martinez", "", "jennifer.m@email.com", "258 Cherry Road, Springfield", "1992-06-14"},
            {"Christopher Lee", "555-0109", "chris.lee@email.com", "369 Ash Drive, Springfield", "1987-02-25"},
            {"Amanda White", "555-0110", "amanda.w@email.com", "741 Poplar Street, Springfield", "1993-10-30"}
        };
        
        for (String[] data : sampleData) {
            try {
                String name = data[0];
                String phone = data[1].isEmpty() ? null : data[1];
                String email = data[2].isEmpty() ? null : data[2];
                String address = data[3];
                LocalDate dob = LocalDate.parse(data[4]);
                
                String[] medicalNotes = {
                    "No known allergies",
                    "Allergic to penicillin",
                    "History of hypertension",
                    "Diabetic - Type 2",
                    "",
                    "Asthma - well controlled",
                    "Previous surgery: Appendectomy 2015",
                    "Family history of heart disease",
                    "No significant medical history",
                    "Chronic back pain"
                };
                
                String notes = medicalNotes[random.nextInt(medicalNotes.length)];
                
                var result = patientService.createPatient(name, phone, email, address, dob, notes);
                if (result.isSuccess()) {
                    createdPatients.add(result.getData());
                    System.out.println("Created patient: " + name);
                } else {
                    System.err.println("Failed to create patient " + name + ": " + result.getMessage());
                }
                
            } catch (Exception e) {
                System.err.println("Error creating patient: " + e.getMessage());
            }
        }
        
        return createdPatients;
    }
    
    /**
     * Generate sample appointments
     */
    private void generateSampleAppointments(List<Patient> patients) {
        if (patients.isEmpty()) {
            System.out.println("No patients available for creating appointments.");
            return;
        }
        
        // Generate appointments for the next 2 weeks
        LocalDate startDate = LocalDate.now().minusDays(3); // Include some past appointments
        LocalDate endDate = LocalDate.now().plusDays(14);
        
        LocalTime[] appointmentTimes = {
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            LocalTime.of(10, 30),
            LocalTime.of(11, 0),
            LocalTime.of(11, 30),
            LocalTime.of(14, 0),
            LocalTime.of(14, 30),
            LocalTime.of(15, 0),
            LocalTime.of(15, 30),
            LocalTime.of(16, 0),
            LocalTime.of(16, 30)
        };
        
        String[] appointmentNotes = {
            "Regular checkup",
            "Follow-up appointment",
            "Blood pressure monitoring",
            "Vaccination - flu shot",
            "Consultation for back pain",
            "Diabetes management",
            "Routine physical examination",
            "Medication review",
            "",
            "Allergy consultation"
        };
        
        Appointment.Status[] statuses = {
            Appointment.Status.SCHEDULED,
            Appointment.Status.SCHEDULED,
            Appointment.Status.SCHEDULED,
            Appointment.Status.COMPLETED,
            Appointment.Status.COMPLETED
        };
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Skip weekends for this sample data
            if (currentDate.getDayOfWeek().getValue() <= 5) {
                
                // Generate 2-5 appointments per day
                int appointmentsPerDay = 2 + random.nextInt(4);
                
                for (int i = 0; i < appointmentsPerDay; i++) {
                    try {
                        Patient patient = patients.get(random.nextInt(patients.size()));
                        LocalTime time = appointmentTimes[random.nextInt(appointmentTimes.length)];
                        String notes = appointmentNotes[random.nextInt(appointmentNotes.length)];
                        
                        // For past appointments, use completed/cancelled status
                        Appointment.Status status;
                        if (currentDate.isBefore(LocalDate.now())) {
                            status = random.nextBoolean() ? Appointment.Status.COMPLETED : 
                                    (random.nextInt(10) == 0 ? Appointment.Status.CANCELLED : Appointment.Status.COMPLETED);
                        } else {
                            status = Appointment.Status.SCHEDULED;
                        }
                        
                        var result = appointmentService.createAppointment(
                            patient.getId(), 
                            null, // No specific service for sample data
                            currentDate, 
                            time, 
                            30, // 30 minute appointments
                            notes
                        );
                        
                        if (result.isSuccess()) {
                            // Update status for past appointments
                            if (!currentDate.isAfter(LocalDate.now()) && status != Appointment.Status.SCHEDULED) {
                                Appointment appointment = result.getData();
                                if (status == Appointment.Status.COMPLETED) {
                                    appointmentService.completeAppointment(appointment.getId(), "Completed successfully");
                                } else if (status == Appointment.Status.CANCELLED) {
                                    appointmentService.cancelAppointment(appointment.getId(), "Patient cancelled");
                                }
                            }
                            
                            System.out.println(String.format("Created appointment: %s on %s at %s", 
                                             patient.getName(), currentDate, time));
                        } else {
                            System.err.println("Failed to create appointment: " + result.getMessage());
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error creating appointment: " + e.getMessage());
                    }
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        System.out.println("Rebelle Test Data Generator");
        System.out.println("===========================");
        
        TestDataGenerator generator = new TestDataGenerator();
        generator.generateSampleData();
        
        System.out.println("Done!");
    }
} 