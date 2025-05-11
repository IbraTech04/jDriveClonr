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
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;

public class AuthController {

    @FXML
    protected void onSignInClicked(ActionEvent event) {
        try {
            // Get credentials
            Credential credential = GoogleOAuthService.authorize();

            // Create drive service and store it in App
            DriveAPIService driveService = new DriveAPIService(credential);
            App.setDriveService(driveService);

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/drive-content.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());

            DriveContentController controller = loader.getController();
            controller.setDriveService(App.getDriveService());

            App.setScene(scene);

        } catch (FileNotFoundException e) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Credentials File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Credentials Not Found");
            alert.setHeaderText("Please select your Google credentials file");
            alert.setContentText("The credentials.json file was not found. Please download it from the Google Cloud Console and select it.");
            alert.showAndWait();
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                GoogleOAuthService.setCredentialsFilePath(file.getAbsolutePath());
                this.onSignInClicked(event);
                return;
            }
            alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Credentials Not Found");
            alert.setHeaderText("Invalid credentials file");
            alert.setContentText("The credentials.json file was not found. Please download it from the Google Cloud Console and select it.");
            alert.showAndWait();
        }

        catch (Exception e) {
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