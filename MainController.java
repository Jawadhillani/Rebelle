package com.rebelle.controllers;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;
import com.rebelle.dao.DatabaseManager;
import com.rebelle.services.PatientService;
import com.rebelle.services.AppointmentService;
import com.rebelle.services.InventoryService;
import com.rebelle.controllers.AppointmentFormController;
import com.rebelle.controllers.PatientFormController;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * MainController - Controls the main application window
 */
public class MainController implements Initializable {
    
    // FXML Controls
    @FXML private TabPane mainTabPane;
    @FXML private Label welcomeLabel;
    @FXML private Label todayAppointmentsLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private Label pendingInvoicesLabel;
    @FXML private Label lowStockLabel;
    @FXML private Button newPatientBtn;
    @FXML private Button newAppointmentBtn;
    @FXML private Button viewScheduleBtn;
    @FXML private TextArea aiChatArea;
    @FXML private TextField aiInputField;
    @FXML private Button aiSendBtn;
    @FXML private Label statusLabel;
    @FXML private Label timeLabel;
    
    private Timeline clockTimeline;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeUI();
        startClock();
        loadDashboardData();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Set welcome message
        welcomeLabel.setText("Welcome to Rebelle Medical Practice");
        
        // Initialize AI chat area
        aiChatArea.setText("AI Assistant: Hello! I'm here to help you manage your medical practice. " +
                          "You can ask me to add patients, schedule appointments, check inventory, and more!\n\n");
        
        // Set status
        updateStatus("Application started successfully");
        
        // Set up AI input field
        aiInputField.setOnAction(e -> handleAIMessage());
    }
    
    /**
     * Start the clock in the status bar
     */
    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
        updateClock(); // Initial update
    }
    
    /**
     * Update the clock display
     */
    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        String timeText = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        timeLabel.setText(timeText);
    }
    
    /**
     * Load dashboard statistics
     */
    private void loadDashboardData() {
        try {
            // Test database connection
            if (DatabaseManager.getInstance().testConnection()) {
                updateStatus("Database connected");
                
                // Load real patient statistics
                loadPatientStatistics();
                
                // Load real appointment statistics
                loadAppointmentStatistics();
                
                // Load real inventory statistics
                loadInventoryStatistics();
                
                // TODO: Load other statistics (invoices)
                // For now, set placeholder values for billing
                pendingInvoicesLabel.setText("0");
                
            } else {
                updateStatus("Database connection failed");
                showAlert("Database Error", "Failed to connect to database", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            updateStatus("Error loading dashboard data");
            showAlert("Error", "Failed to load dashboard data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Load patient statistics for dashboard
     */
    private void loadPatientStatistics() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                PatientService patientService = new PatientService();
                var result = patientService.getPatientStatistics();
                if (result.isSuccess()) {
                    return result.getData().getTotalPatients();
                }
                return 0;
            }
        };
        
        task.setOnSucceeded(e -> {
            Integer patientCount = task.getValue();
            totalPatientsLabel.setText(patientCount.toString());
        });
        
        task.setOnFailed(e -> {
            totalPatientsLabel.setText("0");
        });
        
        new Thread(task).start();
    }
    
    /**
     * Load appointment statistics for dashboard
     */
    private void loadAppointmentStatistics() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                AppointmentService appointmentService = new AppointmentService();
                var result = appointmentService.getAppointmentStatistics();
                if (result.isSuccess()) {
                    return result.getData().getTodaysAppointments();
                }
                return 0;
            }
        };
        
        task.setOnSucceeded(e -> {
            Integer appointmentCount = task.getValue();
            todayAppointmentsLabel.setText(appointmentCount.toString());
        });
        
        task.setOnFailed(e -> {
            todayAppointmentsLabel.setText("0");
        });
        
        new Thread(task).start();
    }
    
    /**
     * Load inventory statistics for dashboard
     */
    private void loadInventoryStatistics() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                InventoryService inventoryService = new InventoryService();
                var result = inventoryService.getInventoryStatistics();
                if (result.isSuccess()) {
                    return result.getData().getLowStockCount();
                }
                return 0;
            }
        };
        
        task.setOnSucceeded(e -> {
            Integer lowStockCount = task.getValue();
            lowStockLabel.setText(lowStockCount.toString());
        });
        
        task.setOnFailed(e -> {
            lowStockLabel.setText("0");
        });
        
        new Thread(task).start();
    }
    
    /**
     * Update status bar message
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    // Event Handlers
    
    @FXML
    private void handleNewPatient() {
        try {
            // Switch to patients tab
            if (mainTabPane != null) {
                mainTabPane.getSelectionModel().select(1); // Patients tab (0=Dashboard, 1=Patients, 2=Appointments)
            }
            
            // Wait a bit for tab to load, then trigger new patient
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/patient-form.fxml"));
                    Scene scene = new Scene(loader.load());
                    scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
                    
                    Stage dialog = new Stage();
                    dialog.setTitle("Add New Patient");
                    dialog.setScene(scene);
                    dialog.initModality(Modality.WINDOW_MODAL);
                    dialog.initOwner(newPatientBtn.getScene().getWindow());
                    dialog.setResizable(false);
                    
                    PatientFormController controller = loader.getController();
                    controller.setEditMode(false);
                    
                    dialog.showAndWait();
                    
                    // Refresh dashboard data after patient creation
                    loadDashboardData();
                    
                } catch (IOException e) {
                    showAlert("UI Error", "Failed to open patient form: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });
            
            updateStatus("Opening new patient form...");
            
        } catch (Exception e) {
            showAlert("Error", "Failed to open new patient form: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleNewAppointment() {
        try {
            // Switch to appointments tab
            if (mainTabPane != null) {
                mainTabPane.getSelectionModel().select(2); // Appointments tab (0=Dashboard, 1=Patients, 2=Appointments)
            }
            
            // Wait a bit for tab to load, then trigger new appointment
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/appointment-form.fxml"));
                    Scene scene = new Scene(loader.load());
                    scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
                    
                    Stage dialog = new Stage();
                    dialog.setTitle("Schedule New Appointment");
                    dialog.setScene(scene);
                    dialog.initModality(Modality.WINDOW_MODAL);
                    dialog.initOwner(newAppointmentBtn.getScene().getWindow());
                    dialog.setResizable(false);
                    
                    AppointmentFormController controller = loader.getController();
                    controller.setEditMode(false);
                    
                    dialog.showAndWait();
                    
                    // Refresh dashboard data after appointment creation
                    loadDashboardData();
                    
                } catch (IOException e) {
                    showAlert("UI Error", "Failed to open appointment form: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });
            
            updateStatus("Opening new appointment form...");
            
        } catch (Exception e) {
            showAlert("Error", "Failed to open new appointment form: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleViewSchedule() {
        try {
            // Switch to appointments tab
            if (mainTabPane != null) {
                mainTabPane.getSelectionModel().select(2); // Appointments tab (0=Dashboard, 1=Patients, 2=Appointments)
            }
            updateStatus("Viewing appointment schedule");
        } catch (Exception e) {
            showAlert("Error", "Failed to open appointment schedule: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleAIMessage() {
        String userMessage = aiInputField.getText().trim();
        if (!userMessage.isEmpty()) {
            // Add user message to chat
            aiChatArea.appendText("You: " + userMessage + "\n");
            
            // Clear input field
            aiInputField.clear();
            
            // TODO: Process AI message
            // For now, show a placeholder response
            String aiResponse = "AI Assistant: I understand you said '" + userMessage + 
                              "'. AI features will be implemented in Phase 4. For now, I'm just echoing your messages!";
            aiChatArea.appendText(aiResponse + "\n\n");
            
            // Scroll to bottom
            aiChatArea.setScrollTop(Double.MAX_VALUE);
            
            updateStatus("AI message processed");
        }
    }
    
    @FXML
    private void handleExit() {
        // Clean shutdown
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
        
        // Close database connection
        DatabaseManager.getInstance().closeConnection();
        
        // Exit application
        System.exit(0);
    }
    
    @FXML
    private void handleHelp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/help-dialog.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            Stage dialog = new Stage();
            dialog.setTitle("Rebelle Help & Tips");
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(welcomeLabel.getScene().getWindow());
            dialog.setResizable(true);
            
            dialog.showAndWait();
            
        } catch (IOException e) {
            showAlert("UI Error", "Failed to open help dialog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleAbout() {
        showAlert("About Rebelle", 
                 "Rebelle Medical Practice Management System\n" +
                 "Version 1.0.0\n\n" +
                 "AI-Powered Medical Practice Management\n" +
                 "Built with JavaFX and SQLite\n\n" +
                 "© 2025 Rebelle Medical Solutions", 
                 Alert.AlertType.INFORMATION);
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }
} 