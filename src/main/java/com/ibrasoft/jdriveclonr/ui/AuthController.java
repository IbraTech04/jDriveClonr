package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.auth.GoogleOAuthService;
import com.ibrasoft.jdriveclonr.service.DriveAPIService;
import com.google.api.client.auth.oauth2.Credential;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

public class AuthController {

    @FXML
    protected void onSignInClicked(ActionEvent event) {
        try {
            // Get credentials
            Credential credential = GoogleOAuthService.authorize();

            // Create drive service
            DriveAPIService driveService = new DriveAPIService(credential);

            // Load FXML and inject service
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/drive-content.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());

            // Get controller and set service
            DriveContentController controller = loader.getController();
            controller.setDriveService(driveService);

            // Set the scene
            App.setScene(scene);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Authentication Error");
            alert.setContentText("Failed to authenticate with Google: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    protected void onBackClicked(ActionEvent event) {
        App.navigateTo("welcome.fxml");
    }
}