package com.rebelle.controllers;

import com.rebelle.models.Expense;
import com.rebelle.services.ExpenseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ExpenseController - Controls the expense list view
 */
public class ExpenseController {
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String> descriptionColumn;
    @FXML private TableColumn<Expense, String> amountColumn;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, String> methodColumn;
    @FXML private TableColumn<Expense, LocalDate> dateColumn;
    @FXML private TableColumn<Expense, String> vendorColumn;
    @FXML private Button newExpenseBtn;
    @FXML private Button editExpenseBtn;
    @FXML private Button deleteExpenseBtn;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField searchField;
    @FXML private Label totalLabel;
    
    private final ExpenseService expenseService;
    private final ObservableList<Expense> expenses;
    
    public ExpenseController() {
        this.expenseService = new ExpenseService();
        this.expenses = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        setupTable();
        setupDatePickers();
        loadExpenses();
        
        // Add listeners
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadExpenses());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadExpenses());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadExpenses());
    }
    
    private void setupTable() {
        // Configure columns
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("formattedAmount"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryDisplayName"));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethodDisplayName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("expenseDate"));
        vendorColumn.setCellValueFactory(new PropertyValueFactory<>("vendor"));
        
        // Add table selection listener
        expenseTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                editExpenseBtn.setDisable(newSelection == null);
                deleteExpenseBtn.setDisable(newSelection == null);
            }
        );
        
        // Set table items
        expenseTable.setItems(expenses);
    }
    
    private void setupDatePickers() {
        // Set default date range to current month
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));
    }
    
    private void loadExpenses() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String searchQuery = searchField.getText().trim();
        
        if (startDate == null || endDate == null) {
            return;
        }
        
        // Load expenses in background
        new Thread(() -> {
            ExpenseService.ServiceResult<List<Expense>> result;
            if (!searchQuery.isEmpty()) {
                result = expenseService.searchExpenses(searchQuery);
            } else {
                result = expenseService.getExpensesByDateRange(startDate, endDate);
            }
            
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                if (result.isSuccess()) {
                    expenses.setAll(result.getData());
                    updateTotal();
                } else {
                    showError("Error", result.getMessage());
                }
            });
        }).start();
    }
    
    private void updateTotal() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate != null && endDate != null) {
            new Thread(() -> {
                ExpenseService.ServiceResult<BigDecimal> result = expenseService.getExpenseTotal(startDate, endDate);
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        totalLabel.setText(String.format("Total: $%.2f", result.getData()));
                    } else {
                        showError("Error", result.getMessage());
                    }
                });
            }).start();
        }
    }
    
    @FXML
    private void handleNewExpense() {
        showExpenseForm(null);
    }
    
    @FXML
    private void handleEditExpense() {
        Expense selectedExpense = expenseTable.getSelectionModel().getSelectedItem();
        if (selectedExpense != null) {
            showExpenseForm(selectedExpense);
        }
    }
    
    @FXML
    private void handleDeleteExpense() {
        Expense selectedExpense = expenseTable.getSelectionModel().getSelectedItem();
        if (selectedExpense != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Expense");
            confirmAlert.setContentText("Are you sure you want to delete this expense?");
            
            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                new Thread(() -> {
                    ExpenseService.ServiceResult<Void> result = expenseService.deleteExpense(selectedExpense.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            expenses.remove(selectedExpense);
                            updateTotal();
                            showInfo("Success", "Expense deleted successfully");
                        } else {
                            showError("Error", result.getMessage());
                        }
                    });
                }).start();
            }
        }
    }
    
    private void showExpenseForm(Expense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExpenseForm.fxml"));
            Parent root = loader.load();
            
            ExpenseFormController controller = loader.getController();
            if (expense != null) {
                controller.setExpense(expense);
            }
            
            Stage stage = new Stage();
            stage.setTitle(expense == null ? "New Expense" : "Edit Expense");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Refresh the expense list after the form is closed
            loadExpenses();
            
        } catch (IOException e) {
            showError("Error", "Could not open expense form: " + e.getMessage());
        }
    }
    
    public void onExpenseSaved() {
        loadExpenses();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 