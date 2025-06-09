package com.rebelle.controllers;

import com.rebelle.models.Payment;
import com.rebelle.services.PaymentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PaymentController - Controls the payment list view
 */
public class PaymentController {
    @FXML private TableView<Payment> paymentTable;
    @FXML private TableColumn<Payment, String> patientColumn;
    @FXML private TableColumn<Payment, String> amountColumn;
    @FXML private TableColumn<Payment, String> methodColumn;
    @FXML private TableColumn<Payment, LocalDate> dateColumn;
    @FXML private TableColumn<Payment, String> descriptionColumn;
    @FXML private Button newPaymentBtn;
    @FXML private Button editPaymentBtn;
    @FXML private Button deletePaymentBtn;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField searchField;
    @FXML private Label totalLabel;
    
    private final PaymentService paymentService;
    private final ObservableList<Payment> payments;
    
    public PaymentController() {
        this.paymentService = new PaymentService();
        this.payments = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        // Set up table columns
        dateColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getPaymentDate()));
        amountColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        methodColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPaymentMethodDisplayName()));
        descriptionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDescription()));
        patientColumn.setCellValueFactory(cellData -> {
            Payment payment = cellData.getValue();
            String patientName = payment.getPatient() != null ? 
                payment.getPatient().getFullName() : "Unknown";
            return new SimpleStringProperty(patientName);
        });
        
        // Set up date pickers
        setupDatePickers();
        
        // Load initial data
        loadPayments();
        updateTotal();
        
        // Add listeners
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadPayments());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadPayments());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadPayments());
    }
    
    private void setupTable() {
        // Configure columns
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patient"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("formattedAmount"));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethodDisplayName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Set up patient column to show patient name
        patientColumn.setCellValueFactory(cellData -> {
            Payment payment = cellData.getValue();
            return new SimpleStringProperty(
                payment.getPatient() != null ? payment.getPatient().getDisplayName() : ""
            );
        });
        
        // Add table selection listener
        paymentTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                editPaymentBtn.setDisable(newSelection == null);
                deletePaymentBtn.setDisable(newSelection == null);
            }
        );
        
        // Set table items
        paymentTable.setItems(payments);
    }
    
    private void setupDatePickers() {
        // Set default date range to current month
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));
    }
    
    private void loadPayments() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String searchQuery = searchField.getText().trim();
        
        if (startDate == null || endDate == null) {
            return;
        }
        
        // Load payments in background
        new Thread(() -> {
            PaymentService.ServiceResult<List<Payment>> result;
            if (!searchQuery.isEmpty()) {
                result = paymentService.searchPayments(searchQuery);
            } else {
                result = paymentService.getPaymentsByDateRange(startDate, endDate);
            }
            
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                if (result.isSuccess()) {
                    payments.setAll(result.getData());
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
                PaymentService.ServiceResult<BigDecimal> result = paymentService.getPaymentTotal(startDate, endDate);
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
    private void handleNewPayment() {
        showPaymentForm(null);
    }
    
    @FXML
    private void handleEditPayment() {
        Payment selectedPayment = paymentTable.getSelectionModel().getSelectedItem();
        if (selectedPayment != null) {
            showPaymentForm(selectedPayment);
        }
    }
    
    @FXML
    private void handleDeletePayment() {
        Payment selectedPayment = paymentTable.getSelectionModel().getSelectedItem();
        if (selectedPayment != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Payment");
            confirmAlert.setContentText("Are you sure you want to delete this payment?");
            
            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                new Thread(() -> {
                    PaymentService.ServiceResult<Void> result = paymentService.deletePayment(selectedPayment.getId());
                    javafx.application.Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            payments.remove(selectedPayment);
                            updateTotal();
                            showInfo("Success", "Payment deleted successfully");
                        } else {
                            showError("Error", result.getMessage());
                        }
                    });
                }).start();
            }
        }
    }
    
    private void showPaymentForm(Payment payment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rebelle/views/payment-form.fxml"));
            Scene scene = new Scene(loader.load());
            
            PaymentFormController controller = loader.getController();
            controller.setParentController(this);
            
            if (payment != null) {
                controller.setPaymentToEdit(payment);
            }
            
            Stage stage = new Stage();
            stage.setTitle(payment == null ? "New Payment" : "Edit Payment");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
        } catch (IOException e) {
            showError("Error", "Failed to open payment form");
        }
    }
    
    public void onPaymentSaved() {
        loadPayments();
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