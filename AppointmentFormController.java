package com.rebelle.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.rebelle.models.Appointment;
import com.rebelle.models.Patient;
import com.rebelle.models.Service;
import com.rebelle.services.AppointmentService;
import com.rebelle.services.PatientService;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AppointmentFormController - Controls the appointment schedule/edit form dialog
 */
public class AppointmentFormController implements Initializable {
    
    // FXML Controls
    @FXML private Label formTitleLabel;
    @FXML private ComboBox<Patient> patientComboBox;
    @FXML private Button newPatientBtn;
    @FXML private ComboBox<Service> serviceComboBox;
    @FXML private DatePicker dateField;
    @FXML private ComboBox<LocalTime> timeComboBox;
    @FXML private ComboBox<Integer> durationComboBox;
    @FXML private Label statusLabel;
    @FXML private ComboBox<Appointment.Status> statusComboBox;
    @FXML private TextArea notesField;
    @FXML private Label conflictLabel;
    @FXML private Label validationLabel;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;
    
    // Services and state
    private AppointmentService appointmentService;
    private PatientService patientService;
    private AppointmentController parentController;
    private Appointment editingAppointment;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        appointmentService = new AppointmentService();
        patientService = new PatientService();
        
        setupForm();
        setupValidation();
        loadFormData();
    }
    
    /**
     * Setup form initial state
     */
    private void setupForm() {
        // Setup patient combo box
        patientComboBox.setConverter(new StringConverter<Patient>() {
            @Override
            public String toString(Patient patient) {
                return patient != null ? patient.getDisplayName() : "";
            }
            
            @Override
            public Patient fromString(String string) {
                return null; // Not needed for display-only combo box
            }
        });
        
        // Setup service combo box
        serviceComboBox.setConverter(new StringConverter<Service>() {
            @Override
            public String toString(Service service) {
                return service != null ? service.getSummary() : "";
            }
            
            @Override
            public Service fromString(String string) {
                return null; // Not needed for display-only combo box
            }
        });
        
        // Setup time combo box with common appointment times
        setupTimeComboBox();
        
        // Setup duration combo box
        setupDurationComboBox();
        
        // Setup status combo box
        statusComboBox.setItems(FXCollections.observableArrayList(Appointment.Status.values()));
        
        // Set default date to today (but not past time)
        dateField.setValue(LocalDate.now());
        
        // Setup text area
        notesField.setWrapText(true);
        
        // Add service selection listener to update duration
        serviceComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldService, newService) -> {
            if (newService != null) {
                durationComboBox.setValue(newService.getDurationMinutes());
            }
        });
    }
    
    /**
     * Setup time combo box with appointment slots
     */
    private void setupTimeComboBox() {
        var timeSlots = FXCollections.<LocalTime>observableArrayList();
        
        // Add time slots from 8:00 AM to 6:00 PM in 30-minute intervals
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(18, 0);
        
        LocalTime currentTime = startTime;
        while (!currentTime.isAfter(endTime)) {
            timeSlots.add(currentTime);
            currentTime = currentTime.plusMinutes(30);
        }
        
        timeComboBox.setItems(timeSlots);
        
        // Set custom string converter for time display
        timeComboBox.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime time) {
                return time != null ? time.format(DateTimeFormatter.ofPattern("h:mm a")) : "";
            }
            
            @Override
            public LocalTime fromString(String string) {
                return null; // Not needed for display-only combo box
            }
        });
    }
    
    /**
     * Setup duration combo box with common durations
     */
    private void setupDurationComboBox() {
        var durations = FXCollections.observableArrayList(15, 30, 45, 60, 90, 120);
        durationComboBox.setItems(durations);
        durationComboBox.setValue(30); // Default 30 minutes
        
        // Set custom string converter for duration display
        durationComboBox.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer duration) {
                if (duration == null) return "";
                if (duration < 60) {
                    return duration + " min";
                } else {
                    int hours = duration / 60;
                    int mins = duration % 60;
                    if (mins == 0) {
                        return hours + (hours == 1 ? " hour" : " hours");
                    } else {
                        return hours + "h " + mins + "m";
                    }
                }
            }
            
            @Override
            public Integer fromString(String string) {
                return null; // Not needed for display-only combo box
            }
        });
    }
    
    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Add listeners for real-time validation
        patientComboBox.valueProperty().addListener((obs, oldPatient, newPatient) -> validateForm());
        dateField.valueProperty().addListener((obs, oldDate, newDate) -> validateForm());
        timeComboBox.valueProperty().addListener((obs, oldTime, newTime) -> {
            validateForm();
            checkForConflicts();
        });
        durationComboBox.valueProperty().addListener((obs, oldDuration, newDuration) -> checkForConflicts());
    }
    
    /**
     * Load form data (patients and services)
     */
    private void loadFormData() {
        // Load patients
        Task<PatientService.ServiceResult<List<Patient>>> patientTask = new Task<>() {
            @Override
            protected PatientService.ServiceResult<List<Patient>> call() {
                return patientService.getAllPatients();
            }
        };
        
        patientTask.setOnSucceeded(e -> {
            PatientService.ServiceResult<List<Patient>> result = patientTask.getValue();
            if (result.isSuccess()) {
                patientComboBox.setItems(FXCollections.observableArrayList(result.getData()));
            }
        });
        
        new Thread(patientTask).start();
        
        // Load services
        Task<AppointmentService.ServiceResult<List<Service>>> serviceTask = new Task<>() {
            @Override
            protected AppointmentService.ServiceResult<List<Service>> call() {
                return appointmentService.getAvailableServices();
            }
        };
        
        serviceTask.setOnSucceeded(e -> {
            AppointmentService.ServiceResult<List<Service>> result = serviceTask.getValue();
            if (result.isSuccess()) {
                serviceComboBox.setItems(FXCollections.observableArrayList(result.getData()));
            }
        });
        
        new Thread(serviceTask).start();
    }
    
    /**
     * Set the parent controller
     */
    public void setParentController(AppointmentController parentController) {
        this.parentController = parentController;
    }
    
    /**
     * Set edit mode (true for editing, false for scheduling)
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (editMode) {
            formTitleLabel.setText("Edit Appointment");
            saveBtn.setText("Update Appointment");
            statusLabel.setVisible(true);
            statusComboBox.setVisible(true);
        } else {
            formTitleLabel.setText("Schedule New Appointment");
            saveBtn.setText("Schedule Appointment");
            statusLabel.setVisible(false);
            statusComboBox.setVisible(false);
        }
    }
    
    /**
     * Set appointment data for editing
     */
    public void setAppointment(Appointment appointment) {
        this.editingAppointment = appointment;
        if (appointment != null) {
            populateForm(appointment);
        }
    }
    
    /**
     * Populate form with appointment data
     */
    private void populateForm(Appointment appointment) {
        // Select patient
        if (appointment.getPatient() != null) {
            patientComboBox.getSelectionModel().select(appointment.getPatient());
        }
        
        // Select service
        if (appointment.getService() != null) {
            serviceComboBox.getSelectionModel().select(appointment.getService());
        }
        
        // Set date and time
        dateField.setValue(appointment.getAppointmentDate());
        timeComboBox.setValue(appointment.getAppointmentTime());
        
        // Set duration
        durationComboBox.setValue(appointment.getDurationMinutes());
        
        // Set status (if in edit mode)
        if (isEditMode) {
            statusComboBox.setValue(appointment.getStatus());
        }
        
        // Set notes
        notesField.setText(appointment.getNotes());
    }
    
    /**
     * Validate form inputs
     */
    private void validateForm() {
        Patient patient = patientComboBox.getValue();
        LocalDate date = dateField.getValue();
        LocalTime time = timeComboBox.getValue();
        
        StringBuilder errors = new StringBuilder();
        
        // Patient validation
        if (patient == null) {
            errors.append("Patient selection is required. ");
        }
        
        // Date validation
        if (date == null) {
            errors.append("Appointment date is required. ");
        } else if (date.isBefore(LocalDate.now())) {
            errors.append("Appointment date cannot be in the past. ");
        }
        
        // Time validation
        if (time == null) {
            errors.append("Appointment time is required. ");
        } else if (date != null && date.equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
            errors.append("Appointment time cannot be in the past. ");
        }
        
        // Update validation label
        if (errors.length() > 0) {
            validationLabel.setText(errors.toString().trim());
            validationLabel.setVisible(true);
            saveBtn.setDisable(true);
        } else {
            validationLabel.setVisible(false);
            saveBtn.setDisable(false);
        }
    }
    
    /**
     * Check for appointment conflicts
     */
    private void checkForConflicts() {
        LocalDate date = dateField.getValue();
        LocalTime time = timeComboBox.getValue();
        Integer duration = durationComboBox.getValue();
        
        if (date == null || time == null || duration == null) {
            conflictLabel.setVisible(false);
            return;
        }
        
        // TODO: Check for conflicts with existing appointments
        // This would require calling the appointment service to check conflicts
        // For now, we'll hide the conflict label
        conflictLabel.setVisible(false);
    }
    
    /**
     * Handle new patient button click
     */
    @FXML
    private void handleNewPatient() {
        // TODO: Open patient form dialog
        showInfo("Feature Coming Soon", "The 'New Patient' feature will be available in the next update.");
    }
    
    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        if (!validateFormForSave()) {
            return;
        }
        
        Patient patient = patientComboBox.getValue();
        Service service = serviceComboBox.getValue();
        LocalDate date = dateField.getValue();
        LocalTime time = timeComboBox.getValue();
        Integer duration = durationComboBox.getValue();
        String notes = notesField.getText().trim();
        
        // Disable save button to prevent double-clicking
        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");
        
        Task<AppointmentService.ServiceResult<?>> task;
        
        if (isEditMode && editingAppointment != null) {
            // Update existing appointment
            Appointment.Status status = statusComboBox.getValue();
            task = new Task<>() {
                @Override
                protected AppointmentService.ServiceResult<?> call() {
                    return appointmentService.updateAppointment(
                        editingAppointment.getId(),
                        patient.getId(),
                        service != null ? service.getId() : null,
                        date, time, duration, status, notes
                    );
                }
            };
        } else {
            // Create new appointment
            task = new Task<>() {
                @Override
                protected AppointmentService.ServiceResult<?> call() {
                    return appointmentService.createAppointment(
                        patient.getId(),
                        service != null ? service.getId() : null,
                        date, time, duration, notes
                    );
                }
            };
        }
        
        task.setOnSucceeded(e -> {
            AppointmentService.ServiceResult<?> result = task.getValue();
            if (result.isSuccess()) {
                // Notify parent controller
                if (parentController != null) {
                    parentController.onAppointmentSaved();
                }
                
                // Close dialog
                closeDialog();
                
                // Show success message
                showInfo("Success", result.getMessage() != null ? result.getMessage() : 
                        (isEditMode ? "Appointment updated successfully" : "Appointment scheduled successfully"));
            } else {
                // Show error and re-enable save button
                showError("Save Failed", result.getMessage());
                resetSaveButton();
            }
        });
        
        task.setOnFailed(e -> {
            showError("Database Error", "Failed to save appointment to database");
            resetSaveButton();
        });
        
        new Thread(task).start();
    }
    
    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        // Check if form has unsaved changes
        if (hasUnsavedChanges()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Unsaved Changes");
            confirmAlert.setHeaderText("Discard Changes?");
            confirmAlert.setContentText("You have unsaved changes. Are you sure you want to cancel?");
            
            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }
        
        closeDialog();
    }
    
    /**
     * Validate form for save operation
     */
    private boolean validateFormForSave() {
        Patient patient = patientComboBox.getValue();
        LocalDate date = dateField.getValue();
        LocalTime time = timeComboBox.getValue();
        
        if (patient == null) {
            showError("Validation Error", "Please select a patient for the appointment.");
            patientComboBox.requestFocus();
            return false;
        }
        
        if (date == null) {
            showError("Validation Error", "Please select an appointment date.");
            dateField.requestFocus();
            return false;
        }
        
        if (time == null) {
            showError("Validation Error", "Please select an appointment time.");
            timeComboBox.requestFocus();
            return false;
        }
        
        if (date.isBefore(LocalDate.now())) {
            showError("Validation Error", "Appointment date cannot be in the past.");
            dateField.requestFocus();
            return false;
        }
        
        if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
            showError("Validation Error", "Appointment time cannot be in the past.");
            timeComboBox.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if form has unsaved changes
     */
    private boolean hasUnsavedChanges() {
        if (isEditMode && editingAppointment != null) {
            // Compare current form values with original appointment data
            Patient selectedPatient = patientComboBox.getValue();
            Service selectedService = serviceComboBox.getValue();
            
            return !java.util.Objects.equals(selectedPatient, editingAppointment.getPatient()) ||
                   !java.util.Objects.equals(selectedService, editingAppointment.getService()) ||
                   !java.util.Objects.equals(dateField.getValue(), editingAppointment.getAppointmentDate()) ||
                   !java.util.Objects.equals(timeComboBox.getValue(), editingAppointment.getAppointmentTime()) ||
                   !java.util.Objects.equals(durationComboBox.getValue(), editingAppointment.getDurationMinutes()) ||
                   !java.util.Objects.equals(statusComboBox.getValue(), editingAppointment.getStatus()) ||
                   !java.util.Objects.equals(notesField.getText().trim(), editingAppointment.getNotes() != null ? editingAppointment.getNotes() : "");
        } else {
            // For new appointment, check if any field has meaningful content
            return patientComboBox.getValue() != null ||
                   serviceComboBox.getValue() != null ||
                   (dateField.getValue() != null && !dateField.getValue().equals(LocalDate.now())) ||
                   timeComboBox.getValue() != null ||
                   !notesField.getText().trim().isEmpty();
        }
    }
    
    /**
     * Reset save button to original state
     */
    private void resetSaveButton() {
        saveBtn.setDisable(false);
        saveBtn.setText(isEditMode ? "Update Appointment" : "Schedule Appointment");
    }
    
    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Show error alert
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show info alert
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Set the appointment to edit
     * @param appointment The appointment to edit
     */
    public void setAppointmentToEdit(Appointment appointment) {
        this.editingAppointment = appointment;
        this.isEditMode = true;
        
        // Populate form fields
        patientComboBox.setValue(appointment.getPatient());
        serviceComboBox.setValue(appointment.getService());
        dateField.setValue(appointment.getAppointmentDate());
        timeComboBox.setValue(appointment.getAppointmentTime());
        durationComboBox.setValue(appointment.getDurationMinutes());
        statusComboBox.setValue(appointment.getStatus());
        notesField.setText(appointment.getNotes());
        
        // Update form title
        formTitleLabel.setText("Edit Appointment");
        
        // Enable status selection in edit mode
        statusComboBox.setDisable(false);
        
        // Validate form
        validateForm();
    }
} 