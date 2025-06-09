package com.rebelle.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.rebelle.models.Patient;
import com.rebelle.services.PatientService;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * PatientFormController - Controls the patient add/edit form dialog
 */
public class PatientFormController implements Initializable {
    
    // FXML Controls
    @FXML private Label formTitleLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private DatePicker dobField;
    @FXML private TextArea addressField;
    @FXML private TextArea medicalNotesField;
    @FXML private Label validationLabel;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;
    
    // Services and state
    private PatientService patientService;
    private PatientController parentController;
    private Patient editingPatient;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        patientService = new PatientService();
        setupForm();
        setupValidation();
    }
    
    /**
     * Setup form initial state
     */
    private void setupForm() {
        // Set up date picker
        dobField.setShowWeekNumbers(false);
        dobField.setValue(null);
        
        // Set text area properties
        addressField.setWrapText(true);
        medicalNotesField.setWrapText(true);
        
        // Focus on name field
        nameField.requestFocus();
    }
    
    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Add listeners for real-time validation
        nameField.textProperty().addListener((obs, oldText, newText) -> validateForm());
        phoneField.textProperty().addListener((obs, oldText, newText) -> validateForm());
        emailField.textProperty().addListener((obs, oldText, newText) -> validateForm());
        dobField.valueProperty().addListener((obs, oldDate, newDate) -> validateForm());
    }
    
    /**
     * Set the parent controller
     */
    public void setParentController(PatientController parentController) {
        this.parentController = parentController;
    }
    
    /**
     * Set edit mode (true for editing, false for adding)
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (editMode) {
            formTitleLabel.setText("Edit Patient");
            saveBtn.setText("Update Patient");
        } else {
            formTitleLabel.setText("Add New Patient");
            saveBtn.setText("Save Patient");
        }
    }
    
    /**
     * Set patient data for editing
     */
    public void setPatient(Patient patient) {
        this.editingPatient = patient;
        if (patient != null) {
            populateForm(patient);
        }
    }
    
    /**
     * Populate form with patient data
     */
    private void populateForm(Patient patient) {
        nameField.setText(patient.getName());
        phoneField.setText(patient.getPhone());
        emailField.setText(patient.getEmail());
        addressField.setText(patient.getAddress());
        medicalNotesField.setText(patient.getMedicalNotes());
        dobField.setValue(patient.getDateOfBirth());
    }
    
    /**
     * Validate form inputs
     */
    private void validateForm() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        LocalDate dob = dobField.getValue();
        
        StringBuilder errors = new StringBuilder();
        
        // Name validation
        if (name.isEmpty()) {
            errors.append("Name is required. ");
        } else if (name.length() < 2) {
            errors.append("Name must be at least 2 characters. ");
        }
        
        // Contact validation
        if (phone.isEmpty() && email.isEmpty()) {
            errors.append("Phone or email is required. ");
        }
        
        // Email validation
        if (!email.isEmpty() && !isValidEmail(email)) {
            errors.append("Invalid email format. ");
        }
        
        // Date of birth validation
        if (dob != null) {
            if (dob.isAfter(LocalDate.now())) {
                errors.append("Date of birth cannot be in the future. ");
            } else if (dob.isBefore(LocalDate.now().minusYears(150))) {
                errors.append("Date of birth cannot be more than 150 years ago. ");
            }
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
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        if (!validateFormForSave()) {
            return;
        }
        
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();
        LocalDate dob = dobField.getValue();
        String medicalNotes = medicalNotesField.getText().trim();
        
        // Disable save button to prevent double-clicking
        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");
        
        Task<PatientService.ServiceResult<?>> task;
        
        if (isEditMode && editingPatient != null) {
            // Update existing patient
            task = new Task<>() {
                @Override
                protected PatientService.ServiceResult<?> call() {
                    return patientService.updatePatient(editingPatient.getId(), name, phone, email, address, dob, medicalNotes);
                }
            };
        } else {
            // Create new patient
            task = new Task<>() {
                @Override
                protected PatientService.ServiceResult<?> call() {
                    return patientService.createPatient(name, phone, email, address, dob, medicalNotes);
                }
            };
        }
        
        task.setOnSucceeded(e -> {
            PatientService.ServiceResult<?> result = task.getValue();
            if (result.isSuccess()) {
                // Notify parent controller
                if (parentController != null) {
                    parentController.onPatientSaved();
                }
                
                // Close dialog
                closeDialog();
                
                // Show success message
                showInfo("Success", result.getMessage() != null ? result.getMessage() : 
                        (isEditMode ? "Patient updated successfully" : "Patient created successfully"));
            } else {
                // Show error and re-enable save button
                showError("Save Failed", result.getMessage());
                resetSaveButton();
            }
        });
        
        task.setOnFailed(e -> {
            showError("Database Error", "Failed to save patient to database");
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
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        
        if (name.isEmpty()) {
            showError("Validation Error", "Patient name is required.");
            nameField.requestFocus();
            return false;
        }
        
        if (phone.isEmpty() && email.isEmpty()) {
            showError("Validation Error", "Please provide at least a phone number or email address.");
            phoneField.requestFocus();
            return false;
        }
        
        if (!email.isEmpty() && !isValidEmail(email)) {
            showError("Validation Error", "Please enter a valid email address.");
            emailField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if form has unsaved changes
     */
    private boolean hasUnsavedChanges() {
        if (isEditMode && editingPatient != null) {
            // Compare current form values with original patient data
            return !nameField.getText().trim().equals(editingPatient.getName() != null ? editingPatient.getName() : "") ||
                   !phoneField.getText().trim().equals(editingPatient.getPhone() != null ? editingPatient.getPhone() : "") ||
                   !emailField.getText().trim().equals(editingPatient.getEmail() != null ? editingPatient.getEmail() : "") ||
                   !addressField.getText().trim().equals(editingPatient.getAddress() != null ? editingPatient.getAddress() : "") ||
                   !medicalNotesField.getText().trim().equals(editingPatient.getMedicalNotes() != null ? editingPatient.getMedicalNotes() : "") ||
                   !java.util.Objects.equals(dobField.getValue(), editingPatient.getDateOfBirth());
        } else {
            // For new patient, check if any field has content
            return !nameField.getText().trim().isEmpty() ||
                   !phoneField.getText().trim().isEmpty() ||
                   !emailField.getText().trim().isEmpty() ||
                   !addressField.getText().trim().isEmpty() ||
                   !medicalNotesField.getText().trim().isEmpty() ||
                   dobField.getValue() != null;
        }
    }
    
    /**
     * Reset save button to original state
     */
    private void resetSaveButton() {
        saveBtn.setDisable(false);
        saveBtn.setText(isEditMode ? "Update Patient" : "Save Patient");
    }
    
    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
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
} 