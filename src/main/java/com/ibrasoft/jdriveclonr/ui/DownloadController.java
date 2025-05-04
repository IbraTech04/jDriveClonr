package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.service.DownloadService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadController implements javafx.fxml.Initializable {

    /* -------- FXML -------- */
    @FXML private Label overallLabel;
    @FXML private Label percentLabel;
    @FXML private Label threadsCountLabel;
    @FXML private ProgressBar overallBar;
    @FXML private ListView<Task<?>> threadList;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;

    /* -------- DI -------- */
    private final DownloadService service;
    private Task<?> downloadTask;
    private final DecimalFormat percentFormat = new DecimalFormat("0.0%");
    private final AtomicInteger fileCount = new AtomicInteger(0);

    public DownloadController() {
        this.service = new DownloadService();
        this.service.setService(App.getDriveService());
    }

    /* -------- init -------- */
    @Override
    public void initialize(URL u, ResourceBundle rb) {
        // Set initial states
        percentLabel.setText("0%");
        threadsCountLabel.setText("0 files");

        // Custom cell factory for download items
        threadList.setCellFactory(lv -> new ListCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            private final Label nameLabel = new Label();
            private final Label statusLabel = new Label();
            private final Label percentLabel = new Label("0%");
            private final HBox layout = new HBox(10);
            private final VBox textLayout = new VBox(2);

            {
                // Configure visual elements
                bar.setPrefWidth(100);
                bar.setPrefHeight(8);
                bar.setStyle("-fx-accent: #1a73e8; -fx-control-inner-background: #e8eaed;");

                nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                nameLabel.setTextFill(Color.valueOf("#202124"));

                statusLabel.setFont(Font.font("System", 12));
                statusLabel.setTextFill(Color.valueOf("#5f6368"));

                percentLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                percentLabel.setTextFill(Color.valueOf("#1a73e8"));

                // Setup layout
                textLayout.getChildren().addAll(nameLabel, statusLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                VBox rightLayout = new VBox(5);
                rightLayout.getChildren().addAll(percentLabel, bar);
                rightLayout.setMinWidth(100);

                layout.getChildren().addAll(textLayout, spacer, rightLayout);
                layout.setPadding(new Insets(10, 5, 10, 5));
                layout.setStyle("-fx-background-color: transparent;");
            }

            @Override
            protected void updateItem(Task<?> task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setGraphic(null);
                } else {
                    // Update task data
                    bar.progressProperty().bind(task.progressProperty());

                    // Extract filename from message if possible
                    String message = task.getMessage();
                    String filename = "File";
                    String status = message;

                    if (message != null && message.contains(":")) {
                        String[] parts = message.split(":", 2);
                        filename = parts[0].trim();
                        if (parts.length > 1) {
                            status = parts[1].trim();
                        }
                    }

                    nameLabel.setText(filename);
                    statusLabel.setText(status);

                    // Update percentage
                    task.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            percentLabel.setText(percentFormat.format(newVal.doubleValue()));
                        }
                    });

                    setGraphic(layout);
                }
            }
        });

        // Button styling
        cancelBtn.setStyle("-fx-background-color: #f1f3f4; -fx-text-fill: #5f6368;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle("-fx-background-color: #e8eaed; -fx-text-fill: #5f6368;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle("-fx-background-color: #f1f3f4; -fx-text-fill: #5f6368;"));

        closeBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #1765cc; -fx-text-fill: white;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white;"));

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
                            progress -> Platform.runLater(() -> {
                                updateProgress(progress, 1);
                                percentLabel.setText(percentFormat.format(progress));
                            }),
                            msg -> {
                                Platform.runLater(() -> {
                                    updateMessage(msg);

                                    // If this is a new file, update the counter
                                    if (msg != null && msg.contains("Downloading")) {
                                        int count = fileCount.incrementAndGet();
                                        threadsCountLabel.setText(count + (count == 1 ? " file" : " files"));
                                    }
                                });
                            }
                    );

                    updateMessage("Download completed");
                    updateProgress(1, 1);
                    Platform.runLater(() -> {
                        percentLabel.setText("100%");
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
        alert.setHeaderText("Download Failed");
        alert.setContentText(message);

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/alert.css").toExternalForm());
        dialogPane.getStyleClass().add("error-dialog");

        alert.showAndWait();
    }
}