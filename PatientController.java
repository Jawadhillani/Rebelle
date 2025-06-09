package com.rebelle.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.rebelle.models.Patient;
import com.rebelle.services.PatientService;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * PatientController - Controls the patient management interface
 */
public class PatientController implements Initializable {
    
    // FXML Controls
    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private Button clearSearchBtn;
    @FXML private Button addPatientBtn;
    @FXML private Button editPatientBtn;
    @FXML private Button deletePatientBtn;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, Integer> idColumn;
    @FXML private TableColumn<Patient, String> nameColumn;
    @FXML private TableColumn<Patient, String> ageColumn;
    @FXML private TableColumn<Patient, String> phoneColumn;
    @FXML private TableColumn<Patient, String> emailColumn;
    @FXML private TableColumn<Patient, String> addressColumn;
    @FXML private TableColumn<Patient, String> createdColumn;
    @FXML private Label statusLabel;
    @FXML private Label patientCountLabel;
    
    // Services
    private PatientService patientService;
    private ObservableList<Patient> patientList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        patientService = new PatientService();
        patientList = FXCollections.observableArrayList();
        
        setupTableColumns();
        setupTableSelection();
        setupSearchField();
        loadPatients();
    }
    
    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        // ID Column
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Name Column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        // Age Column
        ageColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAgeString()));
        
        // Phone Column
        phoneColumn.setCellValueFactory(cellData -> {
            String phone = cellData.getValue().getPhone();
            return new SimpleStringProperty(phone != null ? phone : "");
        });
        
        // Email Column
        emailColumn.setCellValueFactory(cellData -> {
            String email = cellData.getValue().getEmail();
            return new SimpleStringProperty(email != null ? email : "");
        });
        
        // Address Column
        addressColumn.setCellValueFactory(cellData -> {
            String address = cellData.getValue().getAddress();
            return new SimpleStringProperty(address != null ? address : "");
        });
        
        // Created Column
        createdColumn.setCellValueFactory(cellData -> {
            String created = cellData.getValue().getCreatedAt()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            return new SimpleStringProperty(created);
        });
        
        // Bind data to table
        patientTable.setItems(patientList);
        
        // Make table sortable
        patientTable.getSortOrder().add(nameColumn);
    }
    
    /**
     * Setup table selection handling
     */
    private void setupTableSelection() {
        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editPatientBtn.setDisable(!hasSelection);
            deletePatientBtn.setDisable(!hasSelection);
        });
        
        // Set row factory for double-click to edit
        patientTable.setRowFactory(tv -> {
            TableRow<Patient> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditPatient();
                }
            });
            return row;
        });
    }
    
    /**
     * Setup search field
     */
    private void setupSearchField() {
        // Search on Enter key
        searchField.setOnAction(e -> handleSearch());
        
        // Auto-search as user types (with delay)
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() >= 3 || newValue.isEmpty()) {
                handleSearch();
            }
        });
    }
    
    /**
     * Load all patients
     */
    private void loadPatients() {
        updateStatus("Loading patients...");
        
        Task<PatientService.ServiceResult<List<Patient>>> task = new Task<>() {
            @Override
            protected PatientService.ServiceResult<List<Patient>> call() {
                return patientService.getAllPatients();
            }
        };
        
        task.setOnSucceeded(e -> {
            PatientService.ServiceResult<List<Patient>> result = task.getValue();
            if (result.isSuccess()) {
                patientList.setAll(result.getData());
                updatePatientCount();
                updateStatus("Patients loaded successfully");
            } else {
                showError("Failed to load patients", result.getMessage());
                updateStatus("Error loading patients");
            }
        });
        
        task.setOnFailed(e -> {
            showError("Database Error", "Failed to load patients from database");
            updateStatus("Error loading patients");
        });
        
        new Thread(task).start();
    }
    
    /**
     * Search patients
     */
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        updateStatus("Searching patients...");
        
        Task<PatientService.ServiceResult<List<Patient>>> task = new Task<>() {
            @Override
            protected PatientService.ServiceResult<List<Patient>> call() {
                return patientService.searchPatients(searchTerm);
            }
        };
        
        task.setOnSucceeded(e -> {
            PatientService.ServiceResult<List<Patient>> result = task.getValue();
            if (result.isSuccess()) {
                patientList.setAll(result.getData());
                updatePatientCount();
                if (searchTerm.isEmpty()) {
                    updateStatus("All patients loaded");
                } else {
                    updateStatus(String.format("Found %d patients matching '%s'", 
                               result.getData().size(), searchTerm));
                }
            } else {
                showError("Search Error", result.getMessage());
                updateStatus("Search failed");
            }
        });
        
        task.setOnFailed(e -> {
            showError("Search Error", "Failed to search patients");
            updateStatus("Search failed");
        });
        
        new Thread(task).start();
    }
    
    /**
     * Clear search and reload all patients
     */
    @FXML
    private void handleClearSearch() {
        searchField.clear();
        loadPatients();
    }
    
    /**
     * Add new patient
     */
    @FXML
    private void handleAddPatient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/patient-form.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            Stage dialog = new Stage();
            dialog.setTitle("Add New Patient");
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(addPatientBtn.getScene().getWindow());
            dialog.setResizable(false);
            
            PatientFormController controller = loader.getController();
            controller.setParentController(this);
            controller.setEditMode(false);
            
            dialog.showAndWait();
            
        } catch (IOException e) {
            showError("UI Error", "Failed to open patient form: " + e.getMessage());
        }
    }
    
    /**
     * Edit selected patient
     */
    @FXML
    private void handleEditPatient() {
        Patient selectedPatient = patientTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showWarning("No Selection", "Please select a patient to edit.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/patient-form.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            Stage dialog = new Stage();
            dialog.setTitle("Edit Patient");
            dialog.setScene(scene);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(editPatientBtn.getScene().getWindow());
            dialog.setResizable(false);
            
            PatientFormController controller = loader.getController();
            controller.setParentController(this);
            controller.setEditMode(true);
            controller.setPatient(selectedPatient);
            
            dialog.showAndWait();
            
        } catch (IOException e) {
            showError("UI Error", "Failed to open patient form: " + e.getMessage());
        }
    }
    
    /**
     * Delete selected patient
     */
    @FXML
    private void handleDeletePatient() {
        Patient selectedPatient = patientTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showWarning("No Selection", "Please select a patient to delete.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Patient");
        confirmAlert.setContentText(String.format("Are you sure you want to delete patient '%s'?\n\nThis action cannot be undone.", 
                                                 selectedPatient.getName()));
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deletePatient(selectedPatient);
        }
    }
    
    /**
     * Delete patient (called after confirmation)
     */
    private void deletePatient(Patient patient) {
        updateStatus("Deleting patient...");
        
        Task<PatientService.ServiceResult<Void>> task = new Task<>() {
            @Override
            protected PatientService.ServiceResult<Void> call() {
                return patientService.deletePatient(patient.getId());
            }
        };
        
        task.setOnSucceeded(e -> {
            PatientService.ServiceResult<Void> result = task.getValue();
            if (result.isSuccess()) {
                patientList.remove(patient);
                updatePatientCount();
                updateStatus("Patient deleted successfully");
                showInfo("Success", "Patient deleted successfully.");
            } else {
                showError("Deletion Failed", result.getMessage());
                updateStatus("Failed to delete patient");
            }
        });
        
        task.setOnFailed(e -> {
            showError("Database Error", "Failed to delete patient from database");
            updateStatus("Failed to delete patient");
        });
        
        new Thread(task).start();
    }
    
    /**
     * Called by PatientFormController when patient is saved
     */
    public void onPatientSaved() {
        loadPatients(); // Refresh the list
    }
    
    /**
     * Update patient count label
     */
    private void updatePatientCount() {
        int count = patientList.size();
        Platform.runLater(() -> {
            patientCountLabel.setText(count + (count == 1 ? " patient" : " patients"));
        });
    }
    
    /**
     * Update status label
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
            }
        });
    }
    
    /**
     * Show error alert
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Show warning alert
     */
    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Show info alert
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Create table row factory for styling
     */
    public TableRow<Patient> createTableRow(TableView<Patient> tableView) {
        return new TableRow<>();
    }
} 