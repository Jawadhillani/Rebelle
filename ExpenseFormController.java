package com.rebelle.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.rebelle.models.Expense;
import com.rebelle.services.ExpenseService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * ExpenseFormController - Controls the expense form dialog for recording business expenses
 */
public class ExpenseFormController implements Initializable {
    
    // FXML Controls
    @FXML private Label formTitleLabel;
    @FXML private TextField descriptionField;
    @FXML private TextField amountField;
    @FXML private ComboBox<Expense.Category> categoryComboBox;
    @FXML private ComboBox<Expense.PaymentMethod> paymentMethodComboBox;
    @FXML private DatePicker expenseDateField;
    @FXML private TextField vendorField;
    @FXML private TextField receiptNumberField;
    @FXML private TextArea notesField;
    @FXML private Label validationLabel;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;
    
    // Services and state
    private ExpenseService expenseService;
    private ExpenseController parentController;
    private Expense editingExpense;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        expenseService = new ExpenseService();
        
        setupForm();
        setupValidation();
    }
    
    /**
     * Setup form initial state
     */
    private void setupForm() {
        // Setup category combo box
        categoryComboBox.setItems(FXCollections.observableArrayList(Expense.Category.values()));
        categoryComboBox.setConverter(new StringConverter<Expense.Category>() {
            @Override
            public String toString(Expense.Category category) {
                return category != null ? category.getDisplayName() : "";
            }
            
            @Override
            public Expense.Category fromString(String string) {
                return null;
            }
        });
        
        // Setup payment method combo box
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(Expense.PaymentMethod.values()));
        paymentMethodComboBox.setConverter(new StringConverter<Expense.PaymentMethod>() {
            @Override
            public String toString(Expense.PaymentMethod method) {
                return method != null ? method.getDisplayName() : "";
            }
            
            @Override
            public Expense.PaymentMethod fromString(String string) {
                return null;
            }
        });
        
        // Set default payment method
        paymentMethodComboBox.setValue(Expense.PaymentMethod.CREDIT_CARD);
        
        // Set default date to today
        expenseDateField.setValue(LocalDate.now());
        
        // Setup text area
        notesField.setWrapText(true);
        
        // Setup amount field validation
        amountField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldValue);
            }
        });
    }
    
    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Add listeners for real-time validation
        descriptionField.textProperty().addListener((obs, oldDesc, newDesc) -> validateForm());
        amountField.textProperty().addListener((obs, oldAmount, newAmount) -> validateForm());
        categoryComboBox.valueProperty().addListener((obs, oldCat, newCat) -> validateForm());
        paymentMethodComboBox.valueProperty().addListener((obs, oldMethod, newMethod) -> validateForm());
        expenseDateField.valueProperty().addListener((obs, oldDate, newDate) -> validateForm());
        vendorField.textProperty().addListener((obs, oldVendor, newVendor) -> validateForm());
    }
    
    /**
     * Set the parent controller
     */
    public void setParentController(ExpenseController parentController) {
        this.parentController = parentController;
    }
    
    /**
     * Set edit mode (true for editing, false for new expense)
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (editMode) {
            formTitleLabel.setText("Edit Expense");
            saveBtn.setText("Update Expense");
        } else {
            formTitleLabel.setText("Record New Expense");
            saveBtn.setText("Save Expense");
        }
    }
    
    /**
     * Set expense data for editing
     */
    public void setExpense(Expense expense) {
        this.editingExpense = expense;
        if (expense != null) {
            populateForm(expense);
        }
    }
    
    /**
     * Populate form with expense data
     */
    private void populateForm(Expense expense) {
        descriptionField.setText(expense.getDescription());
        amountField.setText(expense.getAmount().toString());
        categoryComboBox.setValue(expense.getCategory());
        paymentMethodComboBox.setValue(expense.getPaymentMethod());
        expenseDateField.setValue(expense.getExpenseDate());
        vendorField.setText(expense.getVendor());
        receiptNumberField.setText(expense.getReceiptNumber());
        notesField.setText(expense.getNotes());
    }
    
    /**
     * Validate form inputs
     */
    private void validateForm() {
        String description = descriptionField.getText();
        String amountText = amountField.getText();
        Expense.Category category = categoryComboBox.getValue();
        Expense.PaymentMethod method = paymentMethodComboBox.getValue();
        LocalDate date = expenseDateField.getValue();
        String vendor = vendorField.getText();
        
        StringBuilder errors = new StringBuilder();
        
        // Description validation
        if (description == null || description.trim().isEmpty()) {
            errors.append("Description is required. ");
        }
        
        // Amount validation
        if (amountText == null || amountText.trim().isEmpty()) {
            errors.append("Expense amount is required. ");
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountText);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.append("Expense amount must be greater than zero. ");
                }
            } catch (NumberFormatException e) {
                errors.append("Invalid expense amount. ");
            }
        }
        
        // Category validation
        if (category == null) {
            errors.append("Expense category is required. ");
        }
        
        // Payment method validation
        if (method == null) {
            errors.append("Payment method is required. ");
        }
        
        // Date validation
        if (date == null) {
            errors.append("Expense date is required. ");
        }
        
        // Vendor validation
        if (vendor == null || vendor.trim().isEmpty()) {
            errors.append("Vendor is required. ");
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
        
        String description = descriptionField.getText().trim();
        BigDecimal amount = new BigDecimal(amountField.getText().trim());
        Expense.Category category = categoryComboBox.getValue();
        Expense.PaymentMethod paymentMethod = paymentMethodComboBox.getValue();
        LocalDate expenseDate = expenseDateField.getValue();
        String vendor = vendorField.getText().trim();
        String receiptNumber = receiptNumberField.getText().trim();
        String notes = notesField.getText().trim();
        
        // Disable save button to prevent double-clicking
        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");
        
        Task<ExpenseService.ServiceResult<?>> task;
        
        if (isEditMode && editingExpense != null) {
            // Update existing expense
            task = new Task<>() {
                @Override
                protected ExpenseService.ServiceResult<?> call() {
                    return expenseService.updateExpense(
                        editingExpense.getId(),
                        description, amount, category, paymentMethod,
                        expenseDate, vendor, receiptNumber, notes
                    );
                }
            };
        } else {
            // Create new expense
            task = new Task<>() {
                @Override
                protected ExpenseService.ServiceResult<?> call() {
                    return expenseService.recordExpense(
                        description, amount, category, paymentMethod,
                        expenseDate, vendor, receiptNumber, notes
                    );
                }
            };
        }
        
        task.setOnSucceeded(e -> {
            ExpenseService.ServiceResult<?> result = task.getValue();
            if (result.isSuccess()) {
                // Notify parent controller
                if (parentController != null) {
                    parentController.onExpenseSaved();
                }
                
                // Close dialog
                closeDialog();
                
                // Show success message
                showInfo("Success", result.getMessage() != null ? result.getMessage() : 
                        (isEditMode ? "Expense updated successfully" : "Expense recorded successfully"));
            } else {
                // Show error and re-enable save button
                showError("Save Failed", result.getMessage());
                resetSaveButton();
            }
        });
        
        task.setOnFailed(e -> {
            showError("Database Error", "Failed to save expense to database");
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
        StringBuilder errors = new StringBuilder();
        
        // Description validation
        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            errors.append("Description is required. ");
        }
        
        // Amount validation
        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            errors.append("Amount is required. ");
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountText);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.append("Amount must be greater than 0. ");
                }
            } catch (NumberFormatException e) {
                errors.append("Invalid amount format. ");
            }
        }
        
        // Category validation
        if (categoryComboBox.getValue() == null) {
            errors.append("Category is required. ");
        }
        
        // Payment method validation
        if (paymentMethodComboBox.getValue() == null) {
            errors.append("Payment method is required. ");
        }
        
        // Date validation
        if (expenseDateField.getValue() == null) {
            errors.append("Date is required. ");
        }
        
        // Vendor validation
        String vendor = vendorField.getText().trim();
        if (vendor.isEmpty()) {
            errors.append("Vendor is required. ");
        }
        
        // Update validation label
        if (errors.length() > 0) {
            validationLabel.setText(errors.toString().trim());
            validationLabel.setVisible(true);
            saveBtn.setDisable(true);
            return false;
        } else {
            validationLabel.setVisible(false);
            saveBtn.setDisable(false);
            return true;
        }
    }
    
    /**
     * Check if form has unsaved changes
     */
    private boolean hasUnsavedChanges() {
        if (isEditMode && editingExpense != null) {
            // Compare current form values with original expense data
            String currentDescription = descriptionField.getText().trim();
            BigDecimal currentAmount = new BigDecimal(amountField.getText());
            Expense.Category selectedCategory = categoryComboBox.getValue();
            Expense.PaymentMethod selectedMethod = paymentMethodComboBox.getValue();
            
            return !java.util.Objects.equals(currentDescription, editingExpense.getDescription()) ||
                   !java.util.Objects.equals(currentAmount, editingExpense.getAmount()) ||
                   !java.util.Objects.equals(selectedCategory, editingExpense.getCategory()) ||
                   !java.util.Objects.equals(selectedMethod, editingExpense.getPaymentMethod()) ||
                   !java.util.Objects.equals(expenseDateField.getValue(), editingExpense.getExpenseDate()) ||
                   !java.util.Objects.equals(vendorField.getText().trim(), editingExpense.getVendor()) ||
                   !java.util.Objects.equals(receiptNumberField.getText().trim(), editingExpense.getReceiptNumber()) ||
                   !java.util.Objects.equals(notesField.getText().trim(), editingExpense.getNotes() != null ? editingExpense.getNotes() : "");
        } else {
            // For new expense, check if any field has meaningful content
            return !descriptionField.getText().trim().isEmpty() ||
                   !amountField.getText().trim().isEmpty() ||
                   categoryComboBox.getValue() != null ||
                   paymentMethodComboBox.getValue() != null ||
                   (expenseDateField.getValue() != null && !expenseDateField.getValue().equals(LocalDate.now())) ||
                   !vendorField.getText().trim().isEmpty() ||
                   !receiptNumberField.getText().trim().isEmpty() ||
                   !notesField.getText().trim().isEmpty();
        }
    }
    
    /**
     * Reset save button to original state
     */
    private void resetSaveButton() {
        saveBtn.setDisable(false);
        saveBtn.setText(isEditMode ? "Update Expense" : "Save Expense");
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
} 