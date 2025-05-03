package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.service.DownloadService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class DownloadController implements javafx.fxml.Initializable {

    /* -------- FXML -------- */
    @FXML private Label overallLabel;
    @FXML private ProgressBar overallBar;
    @FXML private ListView<Task<?>> threadList;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;

    /* -------- DI -------- */
    private final DownloadService service;
    private Task<?> downloadTask;

    public DownloadController() {
        this.service = new DownloadService();
        this.service.setService(App.getDriveService());
    }

    /* -------- init -------- */
    @Override 
    public void initialize(URL u, ResourceBundle rb) {
        // Custom cell shows progress bar + label for each task
        threadList.setCellFactory(lv -> new ListCell<>() {
            private final ProgressBar bar = new ProgressBar(0);

            @Override
            protected void updateItem(Task<?> task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                bar.progressProperty().bind(task.progressProperty());
                setText(task.getMessage());
                setGraphic(bar);
            }
        });

        // Button handlers
        cancelBtn.setOnAction(e -> cancelDownload());
        closeBtn.setOnAction(e -> ((Stage) closeBtn.getScene().getWindow()).close());
        
        // Disable close button until download is finished
        closeBtn.setDisable(true);

        // Start the download process
        startDownload();
    }

    private void startDownload() {
        ObservableList<Task<?>> tasks = FXCollections.observableArrayList();
        threadList.setItems(tasks);

        downloadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Starting download...");
                updateProgress(0, 1);

                try {
                    service.downloadFile(
                        DriveContentController.getSelectedRoot(), 
                        App.getConfig(),
                        progress -> Platform.runLater(() -> updateProgress(progress, 1)),
                        msg -> Platform.runLater(() -> updateMessage(msg))
                    );
                    
                    updateMessage("Download completed");
                    updateProgress(1, 1);
                    Platform.runLater(() -> {
                        closeBtn.setDisable(false);
                        cancelBtn.setDisable(true);
                    });
                } catch (Exception e) {
                    String errorMsg = "Error: " + e.getMessage();
                    updateMessage(errorMsg);
                    Platform.runLater(() -> {
                        closeBtn.setDisable(false);
                        cancelBtn.setDisable(true);
                        showError(errorMsg);
                    });
                    throw e;
                }
                return null;
            }
        };

        downloadTask.messageProperty().addListener((obs, old, newMsg) -> {
            overallLabel.setText(newMsg);
        });

        overallBar.progressProperty().bind(downloadTask.progressProperty());
        tasks.add(downloadTask);

        Thread thread = new Thread(downloadTask, "Download-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void cancelDownload() {
        if (downloadTask != null && !downloadTask.isDone()) {
            service.cancel();
            downloadTask.cancel();
            overallLabel.setText("Download cancelled");
            closeBtn.setDisable(false);
            cancelBtn.setDisable(true);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Download Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}