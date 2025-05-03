package com.ibrasoft.jdriveclonr.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfigScreen implements Initializable {
    @FXML private TextField destinationField;
    @FXML private ComboBox<String> exportFormatBox;
    @FXML private CheckBox longPathFixBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        exportFormatBox.getItems().addAll("Original", "PDF", "DOCX");
    }

    @FXML
    public void onStartClicked(ActionEvent event) {
        String destination = destinationField.getText();
        String format = exportFormatBox.getValue();
        boolean longPathFix = longPathFixBox.isSelected();

        if (destination == null || destination.isEmpty() || format == null) {
            showAlert("Please fill in all required fields.");
            return;
        }

        // TODO: Save to config POJO or context, then switch to TreeView screen
        System.out.println("Destination: " + destination);
        System.out.println("Format: " + format);
        System.out.println("Long Path Fix: " + longPathFix);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Input Required");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}