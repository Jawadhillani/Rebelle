package com.rebelle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import com.rebelle.dao.DatabaseManager;

/**
 * Main JavaFX Application for Rebelle Medical Practice Management System
 */
public class RebelleApplication extends Application {
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            DatabaseManager.getInstance().initializeDatabase();
            
            // Load main window FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            
            // Add CSS styling
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            // Configure primary stage
            primaryStage.setTitle("Rebelle Medical Practice Management");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            // Center the window
            primaryStage.centerOnScreen();
            
            primaryStage.show();
            
        } catch (Exception e) {
            showErrorAlert("Startup Error", "Failed to start Rebelle application", e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void stop() {
        try {
            // Clean shutdown - close database connections
            DatabaseManager.getInstance().closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show error alert dialog
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 