package com.rebelle.controllers;

import com.rebelle.models.InventoryItem;
import com.rebelle.models.Category;
import com.rebelle.services.InventoryService;
import com.rebelle.services.InventoryService.ServiceResult;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class InventoryFormController {
    
    @FXML private Label formTitleLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> unitComboBox;
    @FXML private TextField thresholdField;
    @FXML private TextField costField;
    @FXML private TextField supplierField;
    @FXML private DatePicker expiryDateField;
    @FXML private TextArea notesField;
    @FXML private Label validationLabel;
    @FXML private Button saveBtn;
    
    private final InventoryService inventoryService;
    private Stage stage;
    private InventoryItem currentItem;
    private boolean itemSaved;
    
    // Common units for inventory items
    private static final List<String> COMMON_UNITS = Arrays.asList(
        "pieces", "boxes", "bottles", "tubes", "packs", "units", "kits",
        "ml", "g", "kg", "L", "m", "cm", "pairs", "sets"
    );
    
    public InventoryFormController() {
        this.inventoryService = new InventoryService();
        this.itemSaved = false;
    }
    
    @FXML
    public void initialize() {
        // Initialize category combo box
        categoryComboBox.getItems().addAll(Category.values());
        
        // Initialize unit combo box with common units
        unitComboBox.getItems().addAll(COMMON_UNITS);
        
        // Set up numeric validation for quantity and threshold fields
        setupNumericValidation(quantityField);
        setupNumericValidation(thresholdField);
        
        // Set up currency validation for cost field
        setupCurrencyValidation(costField);
        
        // Set up expiry date validation
        expiryDateField.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffebee;");
                }
            }
        });
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public void setItem(InventoryItem item) {
        this.currentItem = item;
        formTitleLabel.setText("Edit Inventory Item");
        
        // Populate form fields
        nameField.setText(item.getName());
        categoryComboBox.setValue(item.getCategory());
        quantityField.setText(String.valueOf(item.getQuantity()));
        unitComboBox.setValue(item.getUnit());
        thresholdField.setText(String.valueOf(item.getThreshold()));
        costField.setText(item.getCostPerUnit().toString());
        supplierField.setText(item.getSupplier());
        expiryDateField.setValue(item.getExpiryDate());
        notesField.setText(item.getNotes());
    }
    
    public boolean isItemSaved() {
        return itemSaved;
    }
    
    @FXML
    private void handleSave() {
        if (!validateForm()) {
            return;
        }
        
        try {
            String name = nameField.getText().trim();
            Category category = categoryComboBox.getValue();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            String unit = unitComboBox.getValue();
            int threshold = Integer.parseInt(thresholdField.getText().trim());
            BigDecimal costPerUnit = new BigDecimal(costField.getText().trim());
            String supplier = supplierField.getText().trim();
            LocalDate expiryDate = expiryDateField.getValue();
            String notes = notesField.getText().trim();
            
            ServiceResult<InventoryItem> result;
            
            if (currentItem == null) {
                // Create new item
                result = inventoryService.createInventoryItem(
                    name, category, quantity, unit, threshold,
                    costPerUnit, supplier, expiryDate, notes
                );
            } else {
                // Update existing item
                result = inventoryService.updateInventoryItem(
                    currentItem.getId(), name, category, unit,
                    threshold, costPerUnit, supplier, expiryDate, notes
                );
            }
            
            if (result.isSuccess()) {
                itemSaved = true;
                stage.close();
            } else {
                showValidationError(result.getMessage());
            }
            
        } catch (NumberFormatException e) {
            showValidationError("Please enter valid numbers for quantity, threshold, and cost.");
        } catch (Exception e) {
            showValidationError("An error occurred: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        stage.close();
    }
    
    private boolean validateForm() {
        // Clear previous validation message
        validationLabel.setVisible(false);
        
        // Validate required fields
        if (nameField.getText().trim().isEmpty()) {
            showValidationError("Item name is required.");
            return false;
        }
        
        if (categoryComboBox.getValue() == null) {
            showValidationError("Category is required.");
            return false;
        }
        
        if (quantityField.getText().trim().isEmpty()) {
            showValidationError("Initial quantity is required.");
            return false;
        }
        
        if (unitComboBox.getValue() == null || unitComboBox.getValue().trim().isEmpty()) {
            showValidationError("Unit is required.");
            return false;
        }
        
        // Validate numeric fields
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 0) {
                showValidationError("Quantity cannot be negative.");
                return false;
            }
            
            if (!thresholdField.getText().trim().isEmpty()) {
                int threshold = Integer.parseInt(thresholdField.getText().trim());
                if (threshold < 0) {
                    showValidationError("Threshold cannot be negative.");
                    return false;
                }
            }
            
            if (!costField.getText().trim().isEmpty()) {
                BigDecimal cost = new BigDecimal(costField.getText().trim());
                if (cost.compareTo(BigDecimal.ZERO) < 0) {
                    showValidationError("Cost cannot be negative.");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            showValidationError("Please enter valid numbers for quantity, threshold, and cost.");
            return false;
        }
        
        return true;
    }
    
    private void setupNumericValidation(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
    
    private void setupCurrencyValidation(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                field.setText(oldValue);
            }
        });
    }
    
    private void showValidationError(String message) {
        validationLabel.setText(message);
        validationLabel.setVisible(true);
    }
} 