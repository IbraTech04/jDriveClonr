package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.model.ConfigModel;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.model.GoogleMime;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
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
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ConfigController implements Initializable {
    @FXML private TextField destinationField;
    @FXML private ComboBox<ExportFormat> docsFormatBox;
    @FXML private ComboBox<ExportFormat> sheetsFormatBox;
    @FXML private ComboBox<ExportFormat> slidesFormatBox;
    @FXML private ComboBox<ExportFormat> drawingsFormatBox;
    @FXML private ComboBox<ExportFormat> jamboardFormatBox;
    @FXML private Button browseButton;
    @FXML private Button helpButton;
    @FXML private Slider threadCountSlider;
    @FXML private Label threadCountLabel;
    @FXML private Label threadWarningLabel;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ConfigModel config = App.getConfigModel();

        // Populate format boxes
        docsFormatBox.getItems().addAll(ExportFormat.getFormatsForGoogleMime(GoogleMime.DOCS));
        sheetsFormatBox.getItems().addAll(ExportFormat.getFormatsForGoogleMime(GoogleMime.SHEETS));
        slidesFormatBox.getItems().addAll(ExportFormat.getFormatsForGoogleMime(GoogleMime.SLIDES));
        drawingsFormatBox.getItems().addAll(ExportFormat.getFormatsForGoogleMime(GoogleMime.DRAWINGS));
        jamboardFormatBox.getItems().addAll(ExportFormat.getFormatsForGoogleMime(GoogleMime.JAMBOARD));

        // Set default values from config
        docsFormatBox.setValue(config.getExportFormat(GoogleMime.DOCS.getMimeType()));
        sheetsFormatBox.setValue(config.getExportFormat(GoogleMime.SHEETS.getMimeType()));
        slidesFormatBox.setValue(config.getExportFormat(GoogleMime.SLIDES.getMimeType()));
        drawingsFormatBox.setValue(config.getExportFormat(GoogleMime.DRAWINGS.getMimeType()));
        jamboardFormatBox.setValue(config.getExportFormat(GoogleMime.JAMBOARD.getMimeType()));
        
        // Setup thread count slider
        threadCountSlider.setValue(config.getThreadCount());
        updateThreadCountLabel(config.getThreadCount());
        
        threadCountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int threadCount = newVal.intValue();
            updateThreadCountLabel(threadCount);
            
            // Show warning if threads > 5
            threadWarningLabel.setVisible(threadCount > 5);
        });

        // Button bindings
        browseButton.setOnAction(e -> handleBrowseButton());
        helpButton.setOnAction(e -> showHelpWindow());

        // Destination path load
        if (config.getDestinationDirectory() != null) {
            destinationField.setText(config.getDestinationDirectory().toString());
        }

        // Optional: customize combo cell display
        setupComboDisplay(docsFormatBox);
        setupComboDisplay(sheetsFormatBox);
        setupComboDisplay(slidesFormatBox);
        setupComboDisplay(drawingsFormatBox);
        setupComboDisplay(jamboardFormatBox);
    }
    
    /**
     * Updates the thread count label based on the selected value
     * @param threadCount The number of threads
     */
    private void updateThreadCountLabel(int threadCount) {
        threadCountLabel.setText(threadCount + (threadCount == 1 ? " thread" : " threads"));
    }

    private void setupComboDisplay(ComboBox<ExportFormat> comboBox) {
        comboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(ExportFormat item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUiLabel());
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ExportFormat item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUiLabel());
            }
        });
    }

    @FXML
    private void handleBrowseButton() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Destination Folder");
        File selectedDirectory = directoryChooser.showDialog(destinationField.getScene().getWindow());

        if (selectedDirectory != null) {
            destinationField.setText(selectedDirectory.getAbsolutePath());
            App.getConfigModel().setDestinationDirectory(selectedDirectory.toPath());
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

            TextArea helpText = getTextArea();

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

    private static TextArea getTextArea() {
        TextArea helpText = new TextArea();
        helpText.setWrapText(true);
        helpText.setEditable(false);
        helpText.setPrefRowCount(10);
        helpText.setText("""
                DriveClonr can clone the following types of files:
                
                 - Google Docs → Microsoft Word, PDF, Plain Text, or HTML
                 - Google Sheets → Microsoft Excel, PDF, CSV, or HTML
                 - Google Slides → Microsoft PowerPoint, PDF, Plain Text, or HTML
                 - Google Drawings → PNG, JPEG, SVG, or PDF
                 - Google Jamboard → PDF or PNG
 
                 We can clone shared files, owned files, and trashed files. We can also
                 clone entire shared drives.

                However, we cannot clone:
                 - Google Forms
                 - Google Sites
                 - Google Maps/My Maps
                 - Google Keep
                 - Any file which is a shortcut to another file
                """);
        return helpText;
    }

    @FXML
    public void onStartClicked(ActionEvent event) throws Exception {
        if (destinationField.getText().isEmpty()) {
            showAlert("Error", "Please select a destination folder.");
            return;
        }

        if (!FileUtils.checkWindowsRegistryLongPath()){
            showAlert("Error", "Your destination drive doesn't support long paths. This can cause issues if your file names are too long. Please enable long paths in Windows Registry before proceeding.");
        }

        updateConfigModel();

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/downloadView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());
            App.setScene(scene);
        } catch (Exception e) {
            showAlert("Error", "Could not load download view: " + e.getMessage());
        }
    }    private void updateConfigModel() {
        ConfigModel config = App.getConfigModel();
        config.setExportFormat(GoogleMime.DOCS.getMimeType(), docsFormatBox.getValue());
        config.setExportFormat(GoogleMime.SHEETS.getMimeType(), sheetsFormatBox.getValue());
        config.setExportFormat(GoogleMime.SLIDES.getMimeType(), slidesFormatBox.getValue());
        config.setExportFormat(GoogleMime.DRAWINGS.getMimeType(), drawingsFormatBox.getValue());
        config.setExportFormat(GoogleMime.JAMBOARD.getMimeType(), jamboardFormatBox.getValue());
        config.setThreadCount((int) threadCountSlider.getValue());
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void onBackClicked(ActionEvent event) throws IOException {
        // TODO: Implement back navigation
    }
}
