package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.model.FileFailure;
import com.ibrasoft.jdriveclonr.service.DownloadService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for the download view that manages the UI for downloading files from Google Drive.
 * Provides real-time progress updates, file-specific progress tracking, and download management.
 */
public class DownloadController implements javafx.fxml.Initializable {

    // FXML injected controls
    @FXML private Label overallLabel;
    @FXML private Label percentLabel;
    @FXML private Label threadsCountLabel;
    @FXML private ProgressBar overallBar;
    @FXML private ListView<Task<?>> threadList;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;
    @FXML private StackPane emptyStatePane;

    private final ConcurrentLinkedQueue<FileFailure> failedFiles = new ConcurrentLinkedQueue<>();


    // Services and tasks
    private final DownloadService service;
    private Task<?> downloadTask;
    
    // UI state tracking
    private final DecimalFormat percentFormat = new DecimalFormat("0.0%");
    private final AtomicInteger fileCount = new AtomicInteger(0);
    private final ObservableList<Task<?>> activeTasks = FXCollections.observableArrayList();
    private final FilteredList<Task<?>> filteredActiveTasks;
    private final StringProperty statusMessage = new SimpleStringProperty("Preparing download...");
    private long startTime;

    /**
     * Constructor initializes the download service.
     */
    public DownloadController() {
        this.service = new DownloadService();
        this.service.setService(App.getDriveService());
        this.filteredActiveTasks = new FilteredList<>(activeTasks, task -> task.getState() != Worker.State.SUCCEEDED && task.getState() != Worker.State.CANCELLED);
    }

    @Override
    public void initialize(URL u, ResourceBundle rb) {
        initializeUI();
        setupListView();
        setupButtons();
        setupWindowHandlers();
        startDownload();
    }

    /**
     * Initializes basic UI components and bindings.
     */
    private void initializeUI() {
        percentLabel.setText("0%");
        threadsCountLabel.setText("0 files");
        overallLabel.textProperty().bind(statusMessage);
        
        // Style the progress bar
        overallBar.setStyle("-fx-accent: #1967D2;");
        
        // Empty state visibility binding
        emptyStatePane.visibleProperty().bind(Bindings.isEmpty(activeTasks));
        threadList.visibleProperty().bind(Bindings.isNotEmpty(activeTasks));
    }

    /**
     * Configures the ListView with custom cell factory for download items.
     */
    private void setupListView() {
        threadList.setItems(filteredActiveTasks);
        threadList.setCellFactory(lv -> new DownloadCell());
    }

    /**
     * Sets up button handlers and styling.
     */
    private void setupButtons() {
        // Cancel button configuration
        styleButton(cancelBtn, "#F1F3F4", "#5F6368", "#E8EAED");
        cancelBtn.setOnAction(e -> cancelDownload());

        // Close button configuration
        styleButton(closeBtn, "#1967D2", "white", "#1557B0");
        closeBtn.setOnAction(e -> closeWindow());
        closeBtn.setDisable(true);
    }

    /**
     * Applies styling to a button with hover effects.
     */
    private void styleButton(Button button, String bgColor, String textColor, String hoverColor) {
        String baseStyle = String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 6;",
            bgColor, textColor
        );
        String hoverStyle = String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 6;",
            hoverColor, textColor
        );
        
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    /**
     * Sets up window close handlers.
     */
    private void setupWindowHandlers() {
        Platform.runLater(() -> {
            Stage stage = (Stage) overallLabel.getScene().getWindow();
            stage.setOnCloseRequest(event -> cleanup());
        });
    }

    /**
     * Starts the download process.
     */
    private void startDownload() {
        startTime = System.currentTimeMillis();
        downloadTask = createDownloadTask();
        setupDownloadTaskBindings();

        Thread thread = new Thread(downloadTask, "Download-Coordinator");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Creates the main download task.
     */
    private Task<Void> createDownloadTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Starting download...");
                updateProgress(0, 1);

                try {
                    service.downloadFile(
                            DriveContentController.getSelectedRoot(),
                            App.getConfigModel(),
                            progress -> Platform.runLater(() -> {
                                updateProgress(progress, 1);
                                percentLabel.setText(percentFormat.format(progress));
                            }),
                            msg -> Platform.runLater(() -> {
                                updateMessage(msg);
                                statusMessage.set(msg);
                                if (msg != null && msg.contains("Downloading")) {
                                    int count = fileCount.incrementAndGet();
                                    threadsCountLabel.setText(count + (count == 1 ? " file" : " files"));
                                }
                            }),
                            task -> Platform.runLater(() -> {
                                activeTasks.add(task);
                                task.stateProperty().addListener((obs, old, newState) -> {
                                    if (newState == Worker.State.SUCCEEDED ||
                                            newState == Worker.State.FAILED ||
                                            newState == Worker.State.CANCELLED) {
                                        // Let progress reach 100% before removal
                                    }
                                });
                            }),
                            failure -> {
                                // Store the failure and show popup
                                failedFiles.add(failure);
                                Platform.runLater(() -> showFileFailurePopup(failure));
                            }
                    );

                    updateMessage("Download completed");
                    updateProgress(1, 1);
                    onDownloadComplete();
                } catch (Exception e) {
                    handleError("Error: " + e.getMessage());
                    throw e;
                }
                return null;
            }
        };
    }

    private void showFileFailurePopup(FileFailure failure) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("File Download Failed");
        alert.setHeaderText("Failed to download: " + failure.getFileName());

        // Create expandable content for error details
        TextArea textArea = new TextArea(failure.getErrorMessage());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(new Label("Error details:"), 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable content
        alert.getDialogPane().setExpandableContent(expContent);

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/styles/alert.css"))
                        .toExternalForm()
        );
        dialogPane.getStyleClass().add("error-dialog");

        alert.show();
    }

    /**
     * Sets up bindings for the download task.
     */
    private void setupDownloadTaskBindings() {
        downloadTask.messageProperty().addListener((obs, old, newMsg) -> 
            statusMessage.set(newMsg));
        overallBar.progressProperty().bind(downloadTask.progressProperty());
    }    /**
     * Handles download completion.
     */
    private void onDownloadComplete() {
        Platform.runLater(() -> {
            percentLabel.setText("100%");
            long duration = System.currentTimeMillis() - startTime;
            String timeText = formatDuration(duration);
            closeBtn.setDisable(false);
            cancelBtn.setDisable(true);

            int count = fileCount.get();
            int failedCount = failedFiles.size();

            if (failedCount > 0) {
                statusMessage.set("Download completed with " + failedCount + " failures in " + timeText);

                // Show a summary alert for all failures
                showFailureSummaryAlert(count, failedCount, timeText);
            } else {
                statusMessage.set("Download completed in " + timeText);

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Download Complete");
                alert.setHeaderText(null);

                String message = String.format(
                        "Successfully cloned %d %s from Google Drive in %s.",
                        count, count == 1 ? "file" : "files", timeText
                );
                alert.setContentText(message);

                // Style the alert
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.getStylesheets().add(
                        Objects.requireNonNull(getClass().getResource("/styles/alert.css"))
                                .toExternalForm()
                );
                dialogPane.getStyleClass().add("success-dialog");

                // Show the alert
                alert.show();
            }
        });
    }
    /**
     * Formats duration into a human-readable string.
     */
    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    private void showFailureSummaryAlert(int totalCount, int failedCount, String timeText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Download Complete with Failures");
        alert.setHeaderText("Download completed with " + failedCount + " failures");

        String contentText = String.format(
                "Cloned %d of %d files from Google Drive in %s.\n%d files failed to download.",
                totalCount - failedCount, totalCount, timeText, failedCount
        );
        alert.setContentText(contentText);

        // Create expandable content with list of failed files
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);

        StringBuilder failuresText = new StringBuilder();
        int index = 1;
        for (FileFailure failure : failedFiles) {
            failuresText.append(index++).append(". ")
                    .append(failure.getFileName())
                    .append(" - ")
                    .append(failure.getErrorMessage())
                    .append("\n\n");
        }
        textArea.setText(failuresText.toString());

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(new Label("Failed files:"), 0, 0);
        expContent.add(textArea, 0, 1);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        // Set expandable content
        alert.getDialogPane().setExpandableContent(expContent);

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/styles/alert.css"))
                        .toExternalForm()
        );
        dialogPane.getStyleClass().add("warning-dialog");

        // Make the dialog resizable
        alert.setResizable(true);

        alert.showAndWait();
    }

    /**
     * Handles error cases.
     */
    private void handleError(String errorMsg) {
        Platform.runLater(() -> {
            statusMessage.set(errorMsg);
            closeBtn.setDisable(false);
            cancelBtn.setDisable(true);
            showError(errorMsg);
        });
    }

    /**
     * Cancels the current download.
     */
    @FXML
    private void cancelDownload() {
        if (downloadTask != null && !downloadTask.isDone()) {
            service.cancel();
            downloadTask.cancel();
            statusMessage.set("Download cancelled");
            closeBtn.setDisable(false);
            cancelBtn.setDisable(true);
        }
        cleanupTasks();
    }

    /**
     * Closes the window.
     */
    @FXML
    private void closeWindow() {
        cleanup();
        ((Stage) closeBtn.getScene().getWindow()).close();
    }

    /**
     * Cleans up resources before closing.
     */
    private void cleanup() {
        if (downloadTask != null && !downloadTask.isDone()) {
            service.cancel();
            downloadTask.cancel();
        }
        cleanupTasks();
    }

    /**
     * Cleans up active tasks.
     */
    private void cleanupTasks() {
        for (Task<?> task : activeTasks) {
            if (task != null && !task.isDone()) {
                task.cancel();
            }
        }
        activeTasks.clear();
    }

    /**
     * Shows an error dialog.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Download Error");
        alert.setHeaderText("Download Failed");
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
            Objects.requireNonNull(getClass().getResource("/styles/alert.css"))
                .toExternalForm()
        );
        dialogPane.getStyleClass().add("error-dialog");

        alert.showAndWait();
    }

    /**
     * Custom cell implementation for download items.
     */
    private static class DownloadCell extends ListCell<Task<?>> {
        private final ProgressBar bar = new ProgressBar(0);
        private final Label nameLabel = new Label();
        private final Label statusLabel = new Label();
        private final Label percentLabel = new Label("0%");
        private final HBox layout = new HBox(10);
        private final VBox textLayout = new VBox(2);

        public DownloadCell() {
            setupCell();
        }

        private void setupCell() {
            // Progress bar styling
            bar.setPrefWidth(100);
            bar.setPrefHeight(8);
            bar.setStyle("-fx-accent: #1967D2;");

            // Labels styling
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            nameLabel.setTextFill(Color.valueOf("#202124"));

            statusLabel.setFont(Font.font("System", 12));
            statusLabel.setTextFill(Color.valueOf("#5F6368"));

            percentLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            percentLabel.setTextFill(Color.valueOf("#1967D2"));

            // Layout setup
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
                updateCellContent(task);
            }
        }

        private void updateCellContent(Task<?> task) {
            // Reset bindings
            bar.progressProperty().unbind();
            bar.progressProperty().bind(task.progressProperty());

            // Parse message for filename and status
            String message = task.getMessage();
            String[] parts = parseMessage(message);
            String filename = parts[0];
            String status = parts[1];

            nameLabel.setText(filename);
            statusLabel.setText(status);

            // Update percentage
            percentLabel.textProperty().unbind();
            percentLabel.textProperty().bind(
                task.progressProperty().multiply(100).asString("%.1f%%")
            );

            setGraphic(layout);
        }

        private String[] parseMessage(String message) {
            String filename = "File";
            String status = message;

            if (message != null && message.contains(":")) {
                String[] parts = message.split(":", 2);
                filename = parts[0].trim();
                status = parts.length > 1 ? parts[1].trim() : "";
            }

            return new String[]{filename, status};
        }
    }
}