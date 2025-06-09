package com.rebelle.controllers;

import com.rebelle.models.Appointment;
import com.rebelle.models.Service;
import com.rebelle.services.AppointmentService;
import com.rebelle.services.AppointmentService.ServiceResult;
import com.rebelle.services.AppointmentService.AppointmentStats;
import com.rebelle.utils.DateTimeUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the appointment management interface
 */
public class AppointmentController {
    
    @FXML private DatePicker filterDatePicker;
    @FXML private Button filterTodayBtn;
    @FXML private Button filterWeekBtn;
    @FXML private Button clearFilterBtn;
    @FXML private Button scheduleAppointmentBtn;
    @FXML private Button editAppointmentBtn;
    @FXML private Button cancelAppointmentBtn;
    @FXML private Button completeAppointmentBtn;
    
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, Integer> idColumn;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> serviceColumn;
    @FXML private TableColumn<Appointment, String> durationColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, String> notesColumn;
    
    @FXML private Label statusLabel;
    @FXML private Label appointmentCountLabel;
    
    private final AppointmentService appointmentService;
    private final ObservableList<Appointment> appointments;
    
    public AppointmentController() {
        this.appointmentService = new AppointmentService();
        this.appointments = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableSelection();
        loadAppointments();
        updateStatusBar();
    }
    
    private void setupTableColumns() {
        // ID Column
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Date Column
        dateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(DateTimeUtils.formatDate(cellData.getValue().getAppointmentDate())));
        
        // Time Column
        timeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(DateTimeUtils.formatTime(cellData.getValue().getAppointmentTime())));
        
        // Patient Column
        patientColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPatient().getFullName()));
        
        // Service Column
        serviceColumn.setCellValueFactory(cellData -> {
            Service service = cellData.getValue().getService();
            return new SimpleStringProperty(service != null ? service.getName() : "");
        });
        
        // Duration Column
        durationColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDurationMinutes() + " min"));
        
        // Status Column
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));
        
        // Notes Column
        notesColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getNotes()));
        
        // Set table data
        appointmentTable.setItems(appointments);
    }
    
    private void setupTableSelection() {
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editAppointmentBtn.setDisable(!hasSelection);
            cancelAppointmentBtn.setDisable(!hasSelection);
            completeAppointmentBtn.setDisable(!hasSelection);
        });
        
        // Double-click to edit
        appointmentTable.setRowFactory(tv -> {
            TableRow<Appointment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditAppointment();
                }
            });
            return row;
        });
    }
    
    private void loadAppointments() {
        ServiceResult<List<Appointment>> result = appointmentService.getAllAppointments();
        if (result.isSuccess()) {
            appointments.setAll(result.getData());
            updateStatusBar();
        } else {
            showError("Error Loading Appointments", result.getMessage());
        }
    }
    
    private void loadAppointmentsByDate(LocalDate date) {
        ServiceResult<List<Appointment>> result = appointmentService.getAppointmentsByDate(date);
        if (result.isSuccess()) {
            appointments.setAll(result.getData());
            updateStatusBar();
        } else {
            showError("Error Loading Appointments", result.getMessage());
        }
    }
    
    private void updateStatusBar() {
        ServiceResult<AppointmentStats> result = appointmentService.getAppointmentStatistics();
        if (result.isSuccess()) {
            AppointmentStats stats = result.getData();
            appointmentCountLabel.setText(String.format("%d appointments", appointments.size()));
            statusLabel.setText(String.format("Today: %d | Upcoming: %d", 
                stats.getTodaysAppointments(), stats.getUpcomingAppointments()));
        }
    }
    
    @FXML
    private void handleFilterToday() {
        filterDatePicker.setValue(LocalDate.now());
        loadAppointmentsByDate(LocalDate.now());
    }
    
    @FXML
    private void handleFilterWeek() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        ServiceResult<List<Appointment>> result = appointmentService.getAppointmentsByDateRange(weekStart, weekEnd);
        if (result.isSuccess()) {
            appointments.setAll(result.getData());
            updateStatusBar();
        } else {
            showError("Error Loading Appointments", result.getMessage());
        }
    }
    
    @FXML
    private void handleClearFilter() {
        filterDatePicker.setValue(null);
        loadAppointments();
    }
    
    /**
     * Schedule new appointment
     */
    @FXML
    private void handleScheduleAppointment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/appointment-form.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            Stage dialog = new Stage();
            dialog.setTitle("Schedule New Appointment");
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(scheduleAppointmentBtn.getScene().getWindow());
            dialog.setResizable(false);
            
            AppointmentFormController controller = loader.getController();
            dialog.showAndWait();
            
            // Refresh the view after dialog closes
            refreshCurrentView();
            
        } catch (IOException e) {
            showError("UI Error", "Failed to open appointment form: " + e.getMessage());
        }
    }
    
    /**
     * Edit selected appointment
     */
    @FXML
    private void handleEditAppointment() {
        Appointment selectedAppointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment == null) {
            showWarning("No Selection", "Please select an appointment to edit.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/appointment-form.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            Stage dialog = new Stage();
            dialog.setTitle("Edit Appointment");
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(editAppointmentBtn.getScene().getWindow());
            dialog.setResizable(false);
            
            AppointmentFormController controller = loader.getController();
            controller.setAppointmentToEdit(selectedAppointment);
            
            dialog.showAndWait();
            
            // Refresh the view after dialog closes
            refreshCurrentView();
            
        } catch (IOException e) {
            showError("UI Error", "Failed to open appointment form: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancelAppointment() {
        Appointment selectedAppointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Cancel Appointment");
            dialog.setHeaderText("Cancel Appointment #" + selectedAppointment.getId());
            dialog.setContentText("Please enter reason for cancellation:");
            
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(reason -> {
                ServiceResult<Appointment> cancelResult = appointmentService.cancelAppointment(
                    selectedAppointment.getId(), reason);
                
                if (cancelResult.isSuccess()) {
                    loadAppointments();
                    showInfo("Success", "Appointment cancelled successfully.");
                } else {
                    showError("Error", cancelResult.getMessage());
                }
            });
        }
    }
    
    @FXML
    private void handleCompleteAppointment() {
        Appointment selectedAppointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Complete Appointment");
            dialog.setHeaderText("Complete Appointment #" + selectedAppointment.getId());
            dialog.setContentText("Please enter completion notes:");
            
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(notes -> {
                ServiceResult<Appointment> completeResult = appointmentService.completeAppointment(
                    selectedAppointment.getId(), notes);
                
                if (completeResult.isSuccess()) {
                    loadAppointments();
                    showInfo("Success", "Appointment marked as completed.");
                } else {
                    showError("Error", completeResult.getMessage());
                }
            });
        }
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }
    
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }
    
    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }
    
    private void refreshCurrentView() {
        loadAppointments();
    }
    
    /**
     * Handle appointment form completion
     */
    public void onAppointmentSaved() {
        // Refresh appointments table
        loadAppointments();
        
        // Update status bar
        updateStatusBar();
        
        // Show success message
        showInfo("Success", "Appointment saved successfully");
    }
} 