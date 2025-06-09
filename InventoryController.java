package com.rebelle.controllers;

import com.rebelle.models.InventoryItem;
import com.rebelle.models.Category;
import com.rebelle.models.Status;
import com.rebelle.models.InventoryTransaction;
import com.rebelle.services.InventoryService;
import com.rebelle.services.InventoryService.InventoryStats;
import com.rebelle.services.InventoryService.ServiceResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class InventoryController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private ComboBox<Status> statusFilter;
    @FXML private Button addItemBtn;
    @FXML private Button addStockBtn;
    @FXML private Button removeStockBtn;
    @FXML private Button editItemBtn;
    @FXML private Button deleteItemBtn;
    @FXML private Button viewAlertsBtn;
    
    @FXML private TableView<InventoryItem> inventoryTable;
    @FXML private TableColumn<InventoryItem, Integer> idColumn;
    @FXML private TableColumn<InventoryItem, String> nameColumn;
    @FXML private TableColumn<InventoryItem, Category> categoryColumn;
    @FXML private TableColumn<InventoryItem, Integer> quantityColumn;
    @FXML private TableColumn<InventoryItem, String> unitColumn;
    @FXML private TableColumn<InventoryItem, Integer> thresholdColumn;
    @FXML private TableColumn<InventoryItem, Status> statusColumn;
    @FXML private TableColumn<InventoryItem, BigDecimal> costColumn;
    @FXML private TableColumn<InventoryItem, BigDecimal> totalValueColumn;
    @FXML private TableColumn<InventoryItem, String> supplierColumn;
    @FXML private TableColumn<InventoryItem, String> expiryColumn;
    @FXML private TableColumn<InventoryItem, String> lastUpdatedColumn;
    
    @FXML private Label totalItemsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label outOfStockLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label statusLabel;
    @FXML private Label itemCountLabel;
    
    private final InventoryService inventoryService;
    private final ObservableList<InventoryItem> inventoryItems;
    private final FilteredList<InventoryItem> filteredItems;
    private final SortedList<InventoryItem> sortedItems;
    
    public InventoryController() {
        this.inventoryService = new InventoryService();
        this.inventoryItems = FXCollections.observableArrayList();
        this.filteredItems = new FilteredList<>(inventoryItems);
        this.sortedItems = new SortedList<>(filteredItems);
    }
    
    @FXML
    public void initialize() {
        // Initialize table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        costColumn.setCellValueFactory(new PropertyValueFactory<>("costPerUnit"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Set up category filter
        categoryFilter.setItems(FXCollections.observableArrayList(Category.values()));
        categoryFilter.getItems().add(0, null);
        categoryFilter.setValue(null);
        
        // Set up status filter
        statusFilter.setItems(FXCollections.observableArrayList(Status.values()));
        statusFilter.getItems().add(0, null);
        statusFilter.setValue(null);
        
        // Set up table sorting
        inventoryTable.setItems(sortedItems);
        sortedItems.comparatorProperty().bind(inventoryTable.comparatorProperty());
        
        // Add row factory for status-based styling
        inventoryTable.setRowFactory(tv -> new TableRow<InventoryItem>() {
            @Override
            protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    switch (item.getStatus()) {
                        case OUT_OF_STOCK:
                            setStyle("-fx-background-color: #ffcdd2;");
                            break;
                        case LOW_STOCK:
                            setStyle("-fx-background-color: #fff9c4;");
                            break;
                        case EXPIRED:
                            setStyle("-fx-background-color: #ff8a80;");
                            break;
                        case EXPIRING_SOON:
                            setStyle("-fx-background-color: #ffe082;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Set up search and filter listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Set up selection listener
        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            addStockBtn.setDisable(!hasSelection);
            removeStockBtn.setDisable(!hasSelection);
            editItemBtn.setDisable(!hasSelection);
            deleteItemBtn.setDisable(!hasSelection);
        });
        
        // Load initial data
        loadInventoryData();
    }
    
    private void applyFilters() {
        filteredItems.setPredicate(item -> {
            if (item == null) return false;
            
            // Search filter
            String searchText = searchField.getText().toLowerCase();
            if (!searchText.isEmpty() && !item.getName().toLowerCase().contains(searchText)) {
                return false;
            }
            
            // Category filter
            Category selectedCategory = categoryFilter.getValue();
            if (selectedCategory != null && item.getCategory() != selectedCategory) {
                return false;
            }
            
            // Status filter
            Status selectedStatus = statusFilter.getValue();
            if (selectedStatus != null && item.getStatus() != selectedStatus) {
                return false;
            }
            
            return true;
        });
    }
    
    private void loadInventoryData() {
        ServiceResult<List<InventoryItem>> result = inventoryService.getAllInventoryItems();
        if (result.isSuccess()) {
            inventoryItems.setAll(result.getData());
            updateStatusBar();
        } else {
            showError("Error loading inventory", result.getMessage());
        }
    }
    
    private void updateStatusBar() {
        ServiceResult<InventoryStats> statsResult = inventoryService.getInventoryStatistics();
        if (statsResult.isSuccess()) {
            InventoryStats stats = statsResult.getData();
            totalItemsLabel.setText(String.valueOf(stats.getTotalItems()));
            lowStockLabel.setText(String.valueOf(stats.getLowStockCount()));
            outOfStockLabel.setText(String.valueOf(stats.getOutOfStockCount()));
            totalValueLabel.setText(String.format("$%.2f", stats.getTotalValue()));
            itemCountLabel.setText(String.format("%d items", inventoryItems.size()));
        }
    }
    
    @FXML
    private void handleAddItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory-form.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Add New Inventory Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            InventoryFormController controller = loader.getController();
            controller.setStage(stage);
            
            stage.showAndWait();
            
            if (controller.isItemSaved()) {
                loadInventoryData();
            }
        } catch (Exception e) {
            showError("Error", "Failed to open add item form: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleEditItem() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory-form.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Edit Inventory Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            InventoryFormController controller = loader.getController();
            controller.setStage(stage);
            controller.setItem(selectedItem);
            
            stage.showAndWait();
            
            if (controller.isItemSaved()) {
                loadInventoryData();
            }
        } catch (Exception e) {
            showError("Error", "Failed to open edit form: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAddStock() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Add Stock");
        dialog.setHeaderText("Add Stock to " + selectedItem.getName());
        dialog.setContentText("Enter quantity to add:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(quantityStr -> {
            try {
                int quantity = Integer.parseInt(quantityStr);
                ServiceResult<InventoryItem> serviceResult = inventoryService.addStock(
                    selectedItem.getId(), quantity, "Manual restock", null);
                
                if (serviceResult.isSuccess()) {
                    loadInventoryData();
                    showSuccess("Success", serviceResult.getMessage());
                } else {
                    showError("Error", serviceResult.getMessage());
                }
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Please enter a valid number.");
            }
        });
    }
    
    @FXML
    private void handleRemoveStock() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Remove Stock");
        dialog.setHeaderText("Remove Stock from " + selectedItem.getName());
        dialog.setContentText("Enter quantity to remove:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(quantityStr -> {
            try {
                int quantity = Integer.parseInt(quantityStr);
                ServiceResult<InventoryItem> serviceResult = inventoryService.removeStock(
                    selectedItem.getId(), quantity, InventoryTransaction.Reason.USE, null, null);
                
                if (serviceResult.isSuccess()) {
                    loadInventoryData();
                    showSuccess("Success", serviceResult.getMessage());
                } else {
                    showError("Error", serviceResult.getMessage());
                }
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Please enter a valid number.");
            }
        });
    }
    
    @FXML
    private void handleDeleteItem() {
        InventoryItem selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Inventory Item");
        alert.setContentText("Are you sure you want to delete " + selectedItem.getName() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ServiceResult<Void> serviceResult = inventoryService.deleteInventoryItem(selectedItem.getId());
            
            if (serviceResult.isSuccess()) {
                loadInventoryData();
                showSuccess("Success", serviceResult.getMessage());
            } else {
                showError("Error", serviceResult.getMessage());
            }
        }
    }
    
    @FXML
    private void handleViewAlerts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory-alerts.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Inventory Alerts");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            stage.showAndWait();
        } catch (Exception e) {
            showError("Error", "Failed to open alerts view: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilter.setValue(null);
        statusFilter.setValue(null);
        applyFilters();
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showSuccess(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Called when inventory is saved from a child dialog
     */
    public void onInventorySaved() {
        loadInventoryData();
        updateStatusBar();
    }
} 