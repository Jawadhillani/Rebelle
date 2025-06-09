package com.rebelle.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.rebelle.models.InventoryItem;
import com.rebelle.models.InventoryTransaction;
import com.rebelle.services.InventoryService;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * StockAdjustmentController - Controls the stock adjustment form dialog
 */
public class StockAdjustmentController implements Initializable {
    
    // FXML Controls
    @FXML private Label formTitleLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label currentStockLabel;
    @FXML private Label categoryLabel;
    @FXML private Label thresholdLabel;
    @FXML private ToggleGroup actionToggleGroup;
    @FXML private RadioButton addStockRadio;
    @FXML private RadioButton removeStockRadio;
    @FXML private RadioButton adjustStockRadio;
    @FXML private Label quantityLabel;
    @FXML private TextField quantityField;
    @FXML private Label unitLabel;
    @FXML private ComboBox<InventoryTransaction.Reason> reasonComboBox;
    @FXML private TextArea notesField;
    @FXML private VBox previewBox;
    @FXML private Label previewLabel;
    @FXML private Label validationLabel;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;
    
    // Services and state
    private InventoryService inventoryService;
    private InventoryController parentController;
    private InventoryItem inventoryItem;
    private String actionMode; // "add", "remove", or "adjust"
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        inventoryService = new InventoryService();
        setupForm();
        setupValidation();
    }
    
    /**
     * Setup form initial state
     */
    private void setupForm() {
        // Setup reason combo box
        reasonComboBox.setConverter(new StringConverter<InventoryTransaction.Reason>() {
            @Override
            public String toString(InventoryTransaction.Reason reason) {
                return reason != null ? reason.getDisplayName() : "";
            }
            
            @Override
            public InventoryTransaction.Reason fromString(String string) {
                return null;
            }
        });
        
        // Setup text area
        notesField.setWrapText(true);
        
        // Setup action radio buttons
        actionToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            updateActionMode();
            validateForm();
        });
        
        // Setup numeric field
        setupNumericField(quantityField);
        
        // Focus on quantity field
        quantityField.requestFocus();
    }
    
    /**
     * Setup numeric-only input for quantity field
     */
    private void setupNumericField(TextField field) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("[0-9]*")) {
                field.setText(oldValue);
            }
        });
    }
    
    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Add listeners for real-time validation
        quantityField.textProperty().addListener((obs, oldText, newText) -> {
            validateForm();
            updatePreview();
        });
        reasonComboBox.valueProperty().addListener((obs, oldReason, newReason) -> validateForm());
    }
    
    /**
     * Set the parent controller
     */
    public void setParentController(InventoryController parentController) {
        this.parentController = parentController;
    }
    
    /**
     * Set inventory item and action mode
     */
    public void setInventoryItem(InventoryItem item, String mode) {
        this.inventoryItem = item;
        this.actionMode = mode;
        
        if (item != null) {
            populateItemInfo(item);
            setupActionMode(mode);
        }
    }
    
    /**
     * Populate item information
     */
    private void populateItemInfo(InventoryItem item) {
        itemNameLabel.setText(item.getDisplayName());
        currentStockLabel.setText(item.getQuantityWithUnit());
        categoryLabel.setText(item.getCategory().getDisplayName());
        thresholdLabel.setText(String.valueOf(item.getThreshold()));
        unitLabel.setText(item.getUnit());
    }
    
    /**
     * Setup action mode based on initial selection
     */
    private void setupActionMode(String mode) {
        switch (mode.toLowerCase()) {
            case "add":
                addStockRadio.setSelected(true);
                formTitleLabel.setText("Add Stock");
                break;
            case "remove":
                removeStockRadio.setSelected(true);
                formTitleLabel.setText("Remove Stock");
                break;
            default:
                addStockRadio.setSelected(true);
                formTitleLabel.setText("Adjust Stock");
                break;
        }
        updateActionMode();
    }
    
    /**
     * Update action mode based on selected radio button
     */
    private void updateActionMode() {
        if (addStockRadio.isSelected()) {
            quantityLabel.setText("Quantity to Add *:");
            setupReasonOptions(true);
            saveBtn.setText("Add Stock");
        } else if (removeStockRadio.isSelected()) {
            quantityLabel.setText("Quantity to Remove *:");
            setupReasonOptions(false);
            saveBtn.setText("Remove Stock");
        } else if (adjustStockRadio.isSelected()) {
            quantityLabel.setText("New Total Quantity *:");
            setupReasonOptions(true);
            saveBtn.setText("Adjust Stock");
        }
    }
    
    /**
     * Setup reason options based on action type
     */
    private void setupReasonOptions(boolean isAddition) {
        if (isAddition) {
            // Reasons for adding stock
            reasonComboBox.setItems(FXCollections.observableArrayList(
                InventoryTransaction.Reason.RESTOCK,
                InventoryTransaction.Reason.ADJUSTMENT,
                InventoryTransaction.Reason.OTHER
            ));
            reasonComboBox.setValue(InventoryTransaction.Reason.RESTOCK);
        } else {
            // Reasons for removing stock
            reasonComboBox.setItems(FXCollections.observableArrayList(
                InventoryTransaction.Reason.PATIENT_USE,
                InventoryTransaction.Reason.EXPIRED,
                InventoryTransaction.Reason.DAMAGED,
                InventoryTransaction.Reason.LOST,
                InventoryTransaction.Reason.ADJUSTMENT,
                InventoryTransaction.Reason.OTHER
            ));
            reasonComboBox.setValue(InventoryTransaction.Reason.PATIENT_USE);
        }
    }
    
    /**
     * Update preview of the result
     */
    private void updatePreview() {
        String quantityText = quantityField.getText().trim();
        
        if (quantityText.isEmpty() || inventoryItem == null) {
            previewBox.setVisible(false);
            return;
        }
        
        try {
            int quantity = Integer.parseInt(quantityText);
            int currentStock = inventoryItem.getQuantity();
            
            if (addStockRadio.isSelected()) {
                int newStock = currentStock + quantity;
                previewLabel.setText(String.format("New stock will be: %d %s (current: %d)", 
                                                  newStock, inventoryItem.getUnit(), currentStock));
                previewBox.setVisible(true);
            } else if (removeStockRadio.isSelected()) {
                int newStock = currentStock - quantity;
                if (newStock < 0) {
                    previewLabel.setText(String.format("⚠️ Insufficient stock! Available: %d %s, Requested: %d %s", 
                                                      currentStock, inventoryItem.getUnit(), quantity, inventoryItem.getUnit()));
                    previewLabel.setStyle("-fx-text-fill: red;");
                } else {
                    previewLabel.setText(String.format("New stock will be: %d %s (current: %d)", 
                                                      newStock, inventoryItem.getUnit(), currentStock));
                    previewLabel.setStyle("");
                }
                previewBox.setVisible(true);
            } else if (adjustStockRadio.isSelected()) {
                int difference = quantity - currentStock;
                String action = difference > 0 ? "increase" : (difference < 0 ? "decrease" : "no change");
                previewLabel.setText(String.format("Stock will %s from %d to %d %s (%+d)", 
                                                  action, currentStock, quantity, inventoryItem.getUnit(), difference));
                previewBox.setVisible(true);
            }
            
        } catch (NumberFormatException e) {
            previewBox.setVisible(false);
        }
    }
    
    /**
     * Validate form inputs
     */
    private void validateForm() {
        String quantityText = quantityField.getText().trim();
        InventoryTransaction.Reason reason = reasonComboBox.getValue();
        
        StringBuilder errors = new StringBuilder();
        
        // Quantity validation
        if (quantityText.isEmpty()) {
            errors.append("Quantity is required. ");
        } else {
            try {
                int quantity = Integer.parseInt(quantityText);
                if (quantity <= 0) {
                    errors.append("Quantity must be greater than 0. ");
                } else if (removeStockRadio.isSelected() && inventoryItem != null) {
                    // Check if we have enough stock to remove
                    if (quantity > inventoryItem.getQuantity()) {
                        errors.append(String.format("Cannot remove %d %s. Only %d %s available. ", 
                                                   quantity, inventoryItem.getUnit(), 
                                                   inventoryItem.getQuantity(), inventoryItem.getUnit()));
                    }
                }
            } catch (NumberFormatException e) {
                errors.append("Invalid quantity format. ");
            }
        }
        
        // Reason validation
        if (reason == null) {
            errors.append("Reason is required. ");
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
        
        final int quantity = Integer.parseInt(quantityField.getText().trim());
        final InventoryTransaction.Reason reason = reasonComboBox.getValue();
        final String notes = notesField.getText().trim();
        
        // Disable save button to prevent double-clicking
        saveBtn.setDisable(true);
        saveBtn.setText("Processing...");
        
        Task<InventoryService.ServiceResult<InventoryItem>> task;
        
        if (addStockRadio.isSelected()) {
            // Add stock
            task = new Task<>() {
                @Override
                protected InventoryService.ServiceResult<InventoryItem> call() {
                    return inventoryService.addStock(inventoryItem.getId(), quantity, reason.getDisplayName(), notes);
                }
            };
        } else if (removeStockRadio.isSelected()) {
            // Remove stock
            task = new Task<>() {
                @Override
                protected InventoryService.ServiceResult<InventoryItem> call() {
                    return inventoryService.removeStock(inventoryItem.getId(), quantity, reason, null, notes);
                }
            };
        } else {
            // Adjust stock to exact quantity
            task = new Task<>() {
                @Override
                protected InventoryService.ServiceResult<InventoryItem> call() {
                    return inventoryService.adjustStock(inventoryItem.getId(), quantity, notes != null ? notes : "Stock adjustment");
                }
            };
        }
        
        task.setOnSucceeded(e -> {
            InventoryService.ServiceResult<InventoryItem> result = task.getValue();
            if (result.isSuccess()) {
                // Notify parent controller
                if (parentController != null) {
                    parentController.onInventorySaved();
                }
                
                // Close dialog
                closeDialog();
                
                // Show success message
                showInfo("Success", result.getMessage() != null ? result.getMessage() : "Stock adjusted successfully");
            } else {
                // Show error and re-enable save button
                showError("Operation Failed", result.getMessage());
                resetSaveButton();
            }
        });
        
        task.setOnFailed(e -> {
            showError("Database Error", "Failed to update stock in database");
            resetSaveButton();
        });
        
        new Thread(task).start();
    }
    
    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        closeDialog();
    }
    
    /**
     * Validate form for save operation
     */
    private boolean validateFormForSave() {
        String quantityText = quantityField.getText().trim();
        InventoryTransaction.Reason reason = reasonComboBox.getValue();
        
        if (quantityText.isEmpty()) {
            showError("Validation Error", "Please enter the quantity.");
            quantityField.requestFocus();
            return false;
        }
        
        try {
            int quantity = Integer.parseInt(quantityText);
            if (quantity <= 0) {
                showError("Validation Error", "Quantity must be greater than 0.");
                quantityField.requestFocus();
                return false;
            }
            
            if (removeStockRadio.isSelected() && quantity > inventoryItem.getQuantity()) {
                showError("Validation Error", 
                         String.format("Cannot remove %d %s. Only %d %s available.", 
                                     quantity, inventoryItem.getUnit(), 
                                     inventoryItem.getQuantity(), inventoryItem.getUnit()));
                quantityField.requestFocus();
                return false;
            }
            
        } catch (NumberFormatException e) {
            showError("Validation Error", "Please enter a valid quantity number.");
            quantityField.requestFocus();
            return false;
        }
        
        if (reason == null) {
            showError("Validation Error", "Please select a reason for this adjustment.");
            reasonComboBox.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Reset save button to original state
     */
    private void resetSaveButton() {
        saveBtn.setDisable(false);
        if (addStockRadio.isSelected()) {
            saveBtn.setText("Add Stock");
        } else if (removeStockRadio.isSelected()) {
            saveBtn.setText("Remove Stock");
        } else {
            saveBtn.setText("Adjust Stock");
        }
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