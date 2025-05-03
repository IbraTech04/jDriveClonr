package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.model.ConfigModel;
import com.ibrasoft.jdriveclonr.model.MimeTypeMapping;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ConfigScreen implements Initializable {
    @FXML private TextField destinationField;
    @FXML private ComboBox<String> docsFormatBox;
    @FXML private ComboBox<String> sheetsFormatBox;
    @FXML private ComboBox<String> slidesFormatBox;
    @FXML private ComboBox<String> drawingsFormatBox;
    @FXML private ComboBox<String> jamboardFormatBox;
    @FXML private Button browseButton;
    @FXML private Button helpButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize format boxes with available options for each type
        docsFormatBox.getItems().addAll(MimeTypeMapping.getUiValuesForType(MimeTypeMapping.DOCS_MIME_TYPE));
        sheetsFormatBox.getItems().addAll(MimeTypeMapping.getUiValuesForType(MimeTypeMapping.SHEETS_MIME_TYPE));
        slidesFormatBox.getItems().addAll(MimeTypeMapping.getUiValuesForType(MimeTypeMapping.SLIDES_MIME_TYPE));
        drawingsFormatBox.getItems().addAll(MimeTypeMapping.getUiValuesForType(MimeTypeMapping.DRAWING_MIME_TYPE));
        jamboardFormatBox.getItems().addAll(MimeTypeMapping.getUiValuesForType(MimeTypeMapping.JAMBOARD_MIME_TYPE));
        
        // Set default values
        ConfigModel config = App.getConfig();
        docsFormatBox.setValue(config.getUiValueForType(MimeTypeMapping.DOCS_MIME_TYPE));
        sheetsFormatBox.setValue(config.getUiValueForType(MimeTypeMapping.SHEETS_MIME_TYPE));
        slidesFormatBox.setValue(config.getUiValueForType(MimeTypeMapping.SLIDES_MIME_TYPE));
        drawingsFormatBox.setValue(config.getUiValueForType(MimeTypeMapping.DRAWING_MIME_TYPE));
        jamboardFormatBox.setValue(config.getUiValueForType(MimeTypeMapping.JAMBOARD_MIME_TYPE));
        
        // Initialize buttons
        browseButton.setOnAction(e -> handleBrowseButton());
        helpButton.setOnAction(e -> showHelpWindow());

        // Load any existing destination directory
        if (config.getDestinationDirectory() != null) {
            destinationField.setText(config.getDestinationDirectory().toString());
        }
    }
    
    @FXML
    private void handleBrowseButton() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Destination Folder");
        File selectedDirectory = directoryChooser.showDialog(destinationField.getScene().getWindow());
        
        if (selectedDirectory != null) {
            destinationField.setText(selectedDirectory.getAbsolutePath());
            App.getConfig().setDestinationDirectory(selectedDirectory.toPath());
        }
    }
    
    @FXML
    private void showHelpWindow() {
        try {
            Stage helpStage = new Stage();
            VBox content = new VBox(10);
            content.getStyleClass().add("help-content");
            content.setStyle("-fx-padding: 20;");
            
            Label title = new Label("What We Can Clone");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            
            TextArea helpText = new TextArea();
            helpText.setWrapText(true);
            helpText.setEditable(true);
            helpText.setPrefRowCount(10);
            helpText.setText("DriveClonr can clone the following types of files:\n\n" +
                           "- Google Docs → Microsoft Word, PDF, Plain Text, or HTML\n" +
                           "- Google Sheets → Microsoft Excel, PDF, CSV, or HTML\n" +
                           "- Google Slides → Microsoft PowerPoint, PDF, Plain Text, or HTML\n" +
                           "- Google Drawings → PNG, JPEG, SVG, or PDF\n" +
                           "- Google Jamboard → PDF or PNG\n\n" +
                           "Edit this text to provide more details about cloning capabilities.");
            
            content.getChildren().addAll(title, helpText);
            
            Scene scene = new Scene(content, 500, 400);
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());
            
            helpStage.setTitle("Cloning Capabilities");
            helpStage.setScene(scene);
            helpStage.show();
        } catch (Exception e) {
            showAlert("Error", "Could not open help window: " + e.getMessage());
        }
    }

    @FXML
    public void onStartClicked(ActionEvent event) {
        if (destinationField.getText().isEmpty()) {
            showAlert("Error", "Please select a destination folder.");
            return;
        }

        // Save configurations
        updateConfigModel();
        
        try {
            // Load drive content view with service
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/drive-content.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());

            // Get controller and set service
            DriveContentController controller = loader.getController();
            controller.setDriveService(App.getDriveService());

            // Set the scene
            App.setScene(scene);
        } catch (Exception e) {
            showAlert("Error", "Could not load drive content view: " + e.getMessage());
        }
    }
    
    private void updateConfigModel() {
        ConfigModel config = App.getConfig();
        
        // Update export formats using the MimeTypeMapping utility
        config.setExportFormat(MimeTypeMapping.DOCS_MIME_TYPE, docsFormatBox.getValue());
        config.setExportFormat(MimeTypeMapping.SHEETS_MIME_TYPE, sheetsFormatBox.getValue());
        config.setExportFormat(MimeTypeMapping.SLIDES_MIME_TYPE, slidesFormatBox.getValue());
        config.setExportFormat(MimeTypeMapping.DRAWING_MIME_TYPE, drawingsFormatBox.getValue());
        config.setExportFormat(MimeTypeMapping.JAMBOARD_MIME_TYPE, jamboardFormatBox.getValue());
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}