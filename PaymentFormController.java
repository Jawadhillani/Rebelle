package com.rebelle.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.rebelle.models.Payment;
import com.rebelle.models.Patient;
import com.rebelle.services.PaymentService;
import com.rebelle.services.PatientService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * PaymentFormController - Controls the payment record/edit form dialog
 */
public class PaymentFormController implements Initializable {
    
    // FXML Controls
    @FXML private Label formTitleLabel;
    @FXML private ComboBox<Patient> patientComboBox;
    @FXML private Button newPatientBtn;
    @FXML private TextField amountField;
    @FXML private ComboBox<Payment.PaymentMethod> paymentMethodComboBox;
    @FXML private DatePicker paymentDateField;
    @FXML private TextField descriptionField;
    @FXML private TextArea notesField;
    @FXML private Label validationLabel;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;
    
    // Services and state
    private PaymentService paymentService;
    private PatientService patientService;
    private PaymentController parentController;
    private Payment editingPayment;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        paymentService = new PaymentService();
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
        
        // Setup payment method combo box
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(Payment.PaymentMethod.values()));
        paymentMethodComboBox.setConverter(new StringConverter<Payment.PaymentMethod>() {
            @Override
            public String toString(Payment.PaymentMethod method) {
                return method != null ? method.getDisplayName() : "";
            }
            
            @Override
            public Payment.PaymentMethod fromString(String string) {
                return null; // Not needed for display-only combo box
            }
        });
        
        // Setup amount field
        amountField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                return change;
            }
            
            try {
                new BigDecimal(newText);
                return change;
            } catch (NumberFormatException e) {
                return null;
            }
        }));
        
        // Set default date to today
        paymentDateField.setValue(LocalDate.now());
        
        // Setup text area
        notesField.setWrapText(true);
    }
    
    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Add listeners for real-time validation
        patientComboBox.valueProperty().addListener((obs, oldPatient, newPatient) -> validateForm());
        amountField.textProperty().addListener((obs, oldAmount, newAmount) -> validateForm());
        paymentMethodComboBox.valueProperty().addListener((obs, oldMethod, newMethod) -> validateForm());
        paymentDateField.valueProperty().addListener((obs, oldDate, newDate) -> validateForm());
        descriptionField.textProperty().addListener((obs, oldDesc, newDesc) -> validateForm());
    }
    
    /**
     * Load form data (patients)
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
    }
    
    /**
     * Set the parent controller
     */
    public void setParentController(PaymentController parentController) {
        this.parentController = parentController;
    }
    
    /**
     * Set edit mode (true for editing, false for new payment)
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (editMode) {
            formTitleLabel.setText("Edit Payment");
            saveBtn.setText("Update Payment");
        } else {
            formTitleLabel.setText("Record New Payment");
            saveBtn.setText("Record Payment");
        }
    }
    
    /**
     * Set payment data for editing
     */
    public void setPayment(Payment payment) {
        this.editingPayment = payment;
        if (payment != null) {
            populateForm(payment);
        }
    }
    
    /**
     * Populate form with payment data
     */
    private void populateForm(Payment payment) {
        // Select patient
        if (payment.getPatient() != null) {
            patientComboBox.getSelectionModel().select(payment.getPatient());
        }
        
        // Set amount
        amountField.setText(payment.getAmount().toString());
        
        // Set payment method
        paymentMethodComboBox.setValue(payment.getPaymentMethod());
        
        // Set date
        paymentDateField.setValue(payment.getPaymentDate());
        
        // Set description and notes
        descriptionField.setText(payment.getDescription());
        notesField.setText(payment.getNotes());
    }
    
    /**
     * Validate form inputs
     */
    private void validateForm() {
        Patient patient = patientComboBox.getValue();
        String amountText = amountField.getText();
        Payment.PaymentMethod paymentMethod = paymentMethodComboBox.getValue();
        LocalDate paymentDate = paymentDateField.getValue();
        String description = descriptionField.getText().trim();
        
        StringBuilder errors = new StringBuilder();
        
        // Patient validation
        if (patient == null) {
            errors.append("Patient selection is required. ");
        }
        
        // Amount validation
        if (amountText.isEmpty()) {
            errors.append("Payment amount is required. ");
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountText);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.append("Payment amount must be greater than 0. ");
                }
                if (amount.compareTo(new BigDecimal("10000.00")) > 0) {
                    errors.append("Payment amount cannot exceed $10,000.00. ");
                }
            } catch (NumberFormatException e) {
                errors.append("Invalid payment amount. ");
            }
        }
        
        // Payment method validation
        if (paymentMethod == null) {
            errors.append("Payment method is required. ");
        }
        
        // Date validation
        if (paymentDate == null) {
            errors.append("Payment date is required. ");
        } else if (paymentDate.isAfter(LocalDate.now())) {
            errors.append("Payment date cannot be in the future. ");
        } else if (paymentDate.isBefore(LocalDate.now().minusYears(1))) {
            errors.append("Payment date cannot be more than 1 year ago. ");
        }
        
        // Description validation
        if (description.isEmpty()) {
            errors.append("Payment description is required. ");
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
        BigDecimal amount = new BigDecimal(amountField.getText());
        Payment.PaymentMethod paymentMethod = paymentMethodComboBox.getValue();
        LocalDate paymentDate = paymentDateField.getValue();
        String description = descriptionField.getText().trim();
        String notes = notesField.getText().trim();
        
        // Disable save button to prevent double-clicking
        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");
        
        Task<PaymentService.ServiceResult<?>> task;
        
        if (isEditMode && editingPayment != null) {
            // Update existing payment
            task = new Task<>() {
                @Override
                protected PaymentService.ServiceResult<?> call() {
                    return paymentService.updatePayment(
                        editingPayment.getId(),
                        patient.getId(),
                        amount,
                        paymentMethod,
                        paymentDate,
                        description,
                        notes
                    );
                }
            };
        } else {
            // Create new payment
            task = new Task<>() {
                @Override
                protected PaymentService.ServiceResult<?> call() {
                    return paymentService.createPayment(
                        patient.getId(),
                        amount,
                        paymentMethod,
                        paymentDate,
                        description,
                        notes
                    );
                }
            };
        }
        
        task.setOnSucceeded(e -> {
            PaymentService.ServiceResult<?> result = task.getValue();
            if (result.isSuccess()) {
                // Notify parent controller
                if (parentController != null) {
                    parentController.onPaymentSaved();
                }
                
                // Close dialog
                closeDialog();
                
                // Show success message
                showInfo("Success", result.getMessage() != null ? result.getMessage() : 
                        (isEditMode ? "Payment updated successfully" : "Payment recorded successfully"));
            } else {
                // Show error and re-enable save button
                showError("Save Failed", result.getMessage());
                resetSaveButton();
            }
        });
        
        task.setOnFailed(e -> {
            showError("Database Error", "Failed to save payment to database");
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
        String amountText = amountField.getText();
        Payment.PaymentMethod paymentMethod = paymentMethodComboBox.getValue();
        LocalDate paymentDate = paymentDateField.getValue();
        String description = descriptionField.getText().trim();
        
        if (patient == null) {
            showError("Validation Error", "Please select a patient for the payment.");
            patientComboBox.requestFocus();
            return false;
        }
        
        if (amountText.isEmpty()) {
            showError("Validation Error", "Please enter a payment amount.");
            amountField.requestFocus();
            return false;
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Validation Error", "Payment amount must be greater than 0.");
                amountField.requestFocus();
                return false;
            }
            if (amount.compareTo(new BigDecimal("10000.00")) > 0) {
                showError("Validation Error", "Payment amount cannot exceed $10,000.00.");
                amountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Validation Error", "Please enter a valid payment amount.");
            amountField.requestFocus();
            return false;
        }
        
        if (paymentMethod == null) {
            showError("Validation Error", "Please select a payment method.");
            paymentMethodComboBox.requestFocus();
            return false;
        }
        
        if (paymentDate == null) {
            showError("Validation Error", "Please select a payment date.");
            paymentDateField.requestFocus();
            return false;
        }
        
        if (paymentDate.isAfter(LocalDate.now())) {
            showError("Validation Error", "Payment date cannot be in the future.");
            paymentDateField.requestFocus();
            return false;
        }
        
        if (paymentDate.isBefore(LocalDate.now().minusYears(1))) {
            showError("Validation Error", "Payment date cannot be more than 1 year ago.");
            paymentDateField.requestFocus();
            return false;
        }
        
        if (description.isEmpty()) {
            showError("Validation Error", "Please enter a payment description.");
            descriptionField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if form has unsaved changes
     */
    private boolean hasUnsavedChanges() {
        if (isEditMode && editingPayment != null) {
            // Compare current form values with original payment data
            Patient selectedPatient = patientComboBox.getValue();
            String amountText = amountField.getText();
            Payment.PaymentMethod selectedMethod = paymentMethodComboBox.getValue();
            
            try {
                BigDecimal amount = amountText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(amountText);
                
                return !java.util.Objects.equals(selectedPatient, editingPayment.getPatient()) ||
                       !java.util.Objects.equals(amount, editingPayment.getAmount()) ||
                       !java.util.Objects.equals(selectedMethod, editingPayment.getPaymentMethod()) ||
                       !java.util.Objects.equals(paymentDateField.getValue(), editingPayment.getPaymentDate()) ||
                       !java.util.Objects.equals(descriptionField.getText().trim(), editingPayment.getDescription()) ||
                       !java.util.Objects.equals(notesField.getText().trim(), editingPayment.getNotes() != null ? editingPayment.getNotes() : "");
            } catch (NumberFormatException e) {
                return true; // If amount is invalid, consider it changed
            }
        } else {
            // For new payment, check if any field has meaningful content
            return patientComboBox.getValue() != null ||
                   !amountField.getText().isEmpty() ||
                   paymentMethodComboBox.getValue() != null ||
                   (paymentDateField.getValue() != null && !paymentDateField.getValue().equals(LocalDate.now())) ||
                   !descriptionField.getText().trim().isEmpty() ||
                   !notesField.getText().trim().isEmpty();
        }
    }
    
    /**
     * Reset save button to original state
     */
    private void resetSaveButton() {
        saveBtn.setDisable(false);
        saveBtn.setText(isEditMode ? "Update Payment" : "Record Payment");
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
     * Set the payment to edit
     * @param payment The payment to edit
     */
    public void setPaymentToEdit(Payment payment) {
        this.editingPayment = payment;
        this.isEditMode = true;
        
        // Populate form fields
        patientComboBox.setValue(payment.getPatient());
        amountField.setText(payment.getAmount().toString());
        paymentMethodComboBox.setValue(payment.getPaymentMethod());
        paymentDateField.setValue(payment.getPaymentDate());
        descriptionField.setText(payment.getDescription());
        notesField.setText(payment.getNotes());
        
        // Update form title
        formTitleLabel.setText("Edit Payment");
        
        // Validate form
        validateForm();
    }
} 