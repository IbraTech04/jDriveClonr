package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.model.DriveDownloadTask;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.service.DownloadService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Controller for the download view that manages the UI for downloading files from Google Drive.
 * Provides real-time progress updates, file-specific progress tracking, and download management.
 * <p>
 * Updated to work with the new DownloadService structure:
 * - Properly integrates with the service's observable lists
 * - Tracks progress through service state changes
 * - Handles failed downloads with detailed error reporting
 * - Provides clean separation between UI and service logic
 */
public class DownloadController implements javafx.fxml.Initializable {
    @FXML
    private Label overallLabel;
    @FXML
    private Label percentLabel;
    @FXML
    private Label threadsCountLabel;
    @FXML
    private Label failedCountLabel;
    @FXML
    private ProgressBar overallBar;
    @FXML
    private ListView<Task<?>> threadList;
    @FXML
    private ListView<Task<?>> completedList;
    @FXML
    private ListView<Task<?>> failedList;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private StackPane emptyStatePane;
    @FXML
    private HBox failedHeaderBox;
    @FXML
    private HBox completedHeaderBox;
    @FXML
    private Label completedCountLabel;
    @FXML
    private VBox failedDownloadsContainer;
    @FXML
    private StackPane failedDownloadsPane;
    @FXML
    private StackPane completedDownloadsPane;    // Services and state
    private final DownloadService downloadService;
    private final DecimalFormat percentFormat = new DecimalFormat("0.0%");
    private final StringProperty statusMessage = new SimpleStringProperty("Preparing download...");
    private long startTime;

    /**
     * Constructor initializes the download service.
     */
    public DownloadController() {
        // Initialize service with the selected root from DriveContentController
        this.downloadService = new DownloadService(DriveContentController.getSelectedRoot());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeUI();
        setupListView();
        setupButtons();
        setupWindowHandlers();
        setupServiceListeners();
        startDownload();
    }

    /**
     * Initializes basic UI components and bindings.
     */
    private void initializeUI() {
        // Initialize labels
        percentLabel.setText("0%");
        threadsCountLabel.setText("0 files");
        failedCountLabel.setText("0 files");
        completedCountLabel.setText("0 files");
        overallLabel.textProperty().bind(statusMessage);

        // Style the progress bar
        overallBar.setStyle("-fx-accent: #1967D2;");
        overallBar.setProgress(0.0);

        // Set up visibility bindings for active downloads
        emptyStatePane.visibleProperty().bind(Bindings.isEmpty(downloadService.getDownloadTasks()));
        threadList.visibleProperty().bind(Bindings.isNotEmpty(downloadService.getDownloadTasks()));

        // Initially hide completed and failed downloads sections
        completedHeaderBox.setVisible(false);
        completedDownloadsPane.setVisible(false);
        failedHeaderBox.setVisible(false);
        failedDownloadsPane.setVisible(false);
    }

    /**
     * Configures the ListViews with custom cell factory for download items.
     */
    private void setupListView() {
        // Set up active downloads list
        threadList.setItems(downloadService.getDownloadTasks());
        threadList.setCellFactory(lv -> new DownloadCell());

        // Set up completed downloads list
        completedList.setItems(downloadService.getCompletedTasks());
        completedList.setCellFactory(lv -> new DownloadCell());

        // Set up failed downloads list
        failedList.setItems(downloadService.getFailedTasks());
        failedList.setCellFactory(lv -> new DownloadCell());
    }

    /**
     * Sets up button handlers and styling.
     */
    private void setupButtons() {
        // Cancel button
        styleButton(cancelBtn, "#F1F3F4", "#5F6368", "#E8EAED");
        cancelBtn.setOnAction(e -> cancelDownload());

        // Close button
        styleButton(closeBtn, "#1967D2", "white", "#1557B0");
        closeBtn.setOnAction(e -> closeWindow());
        closeBtn.setDisable(true); // Initially disabled until download completes/fails
    }

    /**
     * Applies consistent styling to buttons with hover effects.
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
     * Sets up window close handlers for proper cleanup.
     */
    private void setupWindowHandlers() {
        Platform.runLater(() -> {
            Stage stage = (Stage) overallLabel.getScene().getWindow();
            stage.setOnCloseRequest(event -> cleanup());
        });
    }

    /**
     * Starts the download process and records start time.
     */
    private void startDownload() {
        startTime = System.currentTimeMillis();
        statusMessage.set("Starting download...");
        downloadService.start();
    }

    /**
     * Sets up listeners for the download service to track progress and state changes.
     */
    private void setupServiceListeners() {
        // Listen for service state changes
        downloadService.stateProperty().addListener((obs, oldState, newState) -> {
            Platform.runLater(() -> handleServiceStateChange(newState));
        });        // Listen for completed tasks
        downloadService.getCompletedTasks().addListener(
                (javafx.collections.ListChangeListener<Task<?>>) change -> {
                    Platform.runLater(() -> {
                        // Show completed section if this is the first completion
                        if (!completedHeaderBox.isVisible() && !downloadService.getCompletedTasks().isEmpty()) {
                            completedHeaderBox.setVisible(true);
                            completedDownloadsPane.setVisible(true);
                        }
                        updateProgress();
                    });
                });

        // Listen for failed tasks
        downloadService.getFailedTasks().addListener(
                (javafx.collections.ListChangeListener<Task<?>>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Task<?> failedTask : change.getAddedSubList()) {
                                handleFailedTask(failedTask);
                            }
                        }
                    }
                });

        // Set up periodic progress updates
        javafx.animation.Timeline progressUpdater = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.5), e -> updateProgress())
        );
        progressUpdater.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        progressUpdater.play();
    }

    /**
     * Handles changes in the download service state.
     */
    private void handleServiceStateChange(Worker.State newState) {
        switch (newState) {
            case RUNNING -> statusMessage.set("Downloading files...");
            case SUCCEEDED -> onDownloadComplete();
            case FAILED -> handleServiceError();
            case CANCELLED -> handleServiceCancellation();
            default -> {
            }
        }
    }

    /**
     * Handles a failed download task.
     */
    private void handleFailedTask(Task<?> failedTask) {
        // The task is already in the failed list, just update UI visibility
        Platform.runLater(() -> {
            // Show failed section if this is the first failure
            if (!failedHeaderBox.isVisible() && !downloadService.getFailedTasks().isEmpty()) {
                failedHeaderBox.setVisible(true);
                failedDownloadsPane.setVisible(true);
            }
            updateProgress();
        });
    }

    /**
     * Updates the overall progress display based on service state.
     */
    private void updateProgress() {
        int activeTasks = downloadService.getDownloadTasks().size();
        int completedTasks = downloadService.getCompletedTasks().size();
        int failedTasks = downloadService.getFailedTasks().size();
        int totalTasks = activeTasks + completedTasks + failedTasks;

        if (totalTasks > 0) {
            double progress = (double) (completedTasks + failedTasks) / totalTasks;
            overallBar.setProgress(progress);
            percentLabel.setText(percentFormat.format(progress));

            // Update status message for ongoing downloads
            if (completedTasks + failedTasks < totalTasks) {
                statusMessage.set(String.format("Downloading... %d of %d files completed",
                        completedTasks + failedTasks, totalTasks));
            }
        } else {
            // No tasks yet, show preparing state
            overallBar.setProgress(0.0);
            percentLabel.setText("0%");
        }

        // Update count labels
        threadsCountLabel.setText(String.format("%d files", Math.max(0, activeTasks)));
        completedCountLabel.setText(String.format("%d files", completedTasks));
        failedCountLabel.setText(String.format("%d files", failedTasks));
    }

    /**
     * Handles successful completion of all downloads.
     */
    private void onDownloadComplete() {
        percentLabel.setText("100%");
        long duration = System.currentTimeMillis() - startTime;
        String timeText = formatDuration(duration);

        enableCloseButton();

        int completedCount = downloadService.getCompletedTasks().size();
        int failedCount = downloadService.getFailedTasks().size();
        int totalCount = completedCount + failedCount;

        if (failedCount > 0) {
            statusMessage.set(String.format("Download completed with %d failures in %s", failedCount, timeText));
            showFailureSummaryAlert(totalCount, failedCount, completedCount, timeText);
        } else {
            statusMessage.set("Download completed successfully in " + timeText);
            showSuccessAlert(completedCount, timeText);
        }
    }

    /**
     * Handles service-level errors.
     */
    private void handleServiceError() {
        String errorMsg = downloadService.getException() != null
                ? downloadService.getException().getMessage()
                : "Download service failed";

        statusMessage.set("Download failed: " + errorMsg);
        enableCloseButton();
        showErrorAlert(errorMsg);
    }

    /**
     * Handles service cancellation.
     */
    private void handleServiceCancellation() {
        statusMessage.set("Download cancelled");
        enableCloseButton();
    }

    /**
     * Enables the close button and disables the cancel button.
     */
    private void enableCloseButton() {
        closeBtn.setDisable(false);
        cancelBtn.setDisable(true);
    }

    /**
     * Shows a success alert for completed downloads.
     */
    private void showSuccessAlert(int successCount, String timeText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Download Complete");
        alert.setHeaderText(null);
        alert.setContentText(String.format(
                "Successfully downloaded %d %s from Google Drive in %s.",
                successCount, successCount == 1 ? "file" : "files", timeText
        ));

        styleAlert(alert, "success-dialog");
        alert.show();
    }

    /**
     * Shows a summary alert for downloads with failures.
     */
    private void showFailureSummaryAlert(int totalCount, int failedCount, int successCount, String timeText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Download Complete with Failures");
        alert.setHeaderText(String.format("Download completed with %d failures", failedCount));
        alert.setContentText(String.format(
                "Successfully downloaded %d of %d files from Google Drive in %s.\n%d files failed to download.",
                successCount, totalCount, timeText, failedCount
        ));

        // Add expandable content with failed files list
        TextArea failureList = new TextArea();
        failureList.setEditable(false);
        failureList.setWrapText(true);

        StringBuilder failuresText = new StringBuilder();
        int i = 1;
        for (Task<?> failedTask : downloadService.getFailedTasks()) {
            if (failedTask instanceof DriveDownloadTask downloadTask) {
                String fileName = downloadTask.getDriveItem().getName();
                String errorMessage = failedTask.getException() != null
                        ? failedTask.getException().getMessage()
                        : "Unknown error occurred";
                failuresText.append(String.format("%d. %s - %s\n\n", i++, fileName, errorMessage));
            }
        }
        failureList.setText(failuresText.toString());

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(new Label("Failed files:"), 0, 0);
        expContent.add(failureList, 0, 1);

        GridPane.setVgrow(failureList, Priority.ALWAYS);
        GridPane.setHgrow(failureList, Priority.ALWAYS);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.setResizable(true);

        styleAlert(alert, "warning-dialog");
        alert.showAndWait();
    }

    /**
     * Shows an error alert.
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Download Error");
        alert.setHeaderText("Download Failed");
        alert.setContentText(message);

        styleAlert(alert, "error-dialog");
        alert.showAndWait();
    }

    /**
     * Applies consistent styling to alerts.
     */
    private void styleAlert(Alert alert, String styleClass) {
        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/styles/alert.css"))
                            .toExternalForm()
            );
            dialogPane.getStyleClass().add(styleClass);
        } catch (Exception e) {
            // Fallback if stylesheet is not available
            System.err.println("Could not load alert stylesheet: " + e.getMessage());
        }
    }

    /**
     * Formats duration into a human-readable string.
     */
    private String formatDuration(long millis) {
        java.time.Duration duration = java.time.Duration.ofMillis(millis);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    /**
     * Cancels the download and cleans up resources.
     */
    @FXML
    private void cancelDownload() {
        if (downloadService != null && downloadService.isRunning()) {
            downloadService.cancel();
            statusMessage.set("Cancelling download...");
        }
        enableCloseButton();
    }

    /**
     * Closes the window after cleanup.
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
        if (downloadService != null && downloadService.isRunning()) {
            downloadService.cancel();
        }

        // The service will handle cleanup of its executor service
        // when it's cancelled or completed
    }

    /**
     * Custom cell implementation for displaying download tasks in the ListView.
     */
    private static class DownloadCell extends ListCell<Task<?>> {
        private final ProgressBar progressBar = new ProgressBar(0);
        private final Label nameLabel = new Label();
        private final Label statusLabel = new Label();
        private final Label percentLabel = new Label("0%");
        private final HBox layout = new HBox(10);
        private final VBox textLayout = new VBox(2);

        public DownloadCell() {
            setupCell();
        }

        private void setupCell() {
            // Configure progress bar
            progressBar.setPrefWidth(100);
            progressBar.setPrefHeight(8);
            progressBar.setStyle("-fx-accent: #1967D2;");

            // Configure labels
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            nameLabel.setTextFill(Color.valueOf("#202124"));

            statusLabel.setFont(Font.font("System", 12));
            statusLabel.setTextFill(Color.valueOf("#5F6368"));

            percentLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            percentLabel.setTextFill(Color.valueOf("#1967D2"));

            // Setup layout
            textLayout.getChildren().addAll(nameLabel, statusLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox rightLayout = new VBox(5);
            rightLayout.getChildren().addAll(percentLabel, progressBar);
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
                // Unbind any previous bindings
                progressBar.progressProperty().unbind();
                percentLabel.textProperty().unbind();
            } else {
                updateCellContent(task);
                setGraphic(layout);
            }
        }

        private void updateCellContent(Task<?> task) {
            // Unbind previous bindings
            progressBar.progressProperty().unbind();
            percentLabel.textProperty().unbind();

            // Update labels based on task type and state
            if (task instanceof DriveDownloadTask downloadTask) {
                DriveItem item = downloadTask.getDriveItem();
                nameLabel.setText(item.getName());

                // Update status and styling based on task state
                String status = task.getMessage();
                Worker.State state = task.getState();

                if (status == null || status.isEmpty()) {
                    switch (state) {
                        case READY -> status = "Ready to download";
                        case SCHEDULED -> status = "Scheduled";
                        case RUNNING -> status = "Downloading...";
                        case SUCCEEDED -> status = "Completed successfully";
                        case CANCELLED -> status = "Cancelled";
                        case FAILED -> {
                            status = "Failed";
                            if (task.getException() != null) {
                                String errorMsg = task.getException().getMessage();
                                if (errorMsg != null && !errorMsg.isEmpty()) {
                                    status = "Failed: " + errorMsg;
                                }
                            }
                        }
                        default -> status = "Unknown";
                    }
                }
                statusLabel.setText(status);

                // Apply styling based on state
                switch (state) {
                    case SUCCEEDED -> {
                        progressBar.setStyle("-fx-accent: #137333;");
                        percentLabel.setTextFill(Color.valueOf("#137333"));
                        statusLabel.setTextFill(Color.valueOf("#137333"));
                        progressBar.setProgress(1.0);
                        percentLabel.setText("100%");
                    }
                    case FAILED -> {
                        progressBar.setStyle("-fx-accent: #D93025;");
                        percentLabel.setTextFill(Color.valueOf("#D93025"));
                        statusLabel.setTextFill(Color.valueOf("#D93025"));
                        progressBar.setProgress(1.0);
                        percentLabel.setText("Failed");
                    }
                    case RUNNING -> {
                        progressBar.setStyle("-fx-accent: #1967D2;");
                        percentLabel.setTextFill(Color.valueOf("#1967D2"));
                        statusLabel.setTextFill(Color.valueOf("#5F6368"));
                        // Bind progress for running tasks
                        progressBar.progressProperty().bind(task.progressProperty());
                        percentLabel.textProperty().bind(
                                task.progressProperty().multiply(100).asString("%.1f%%")
                        );
                    }
                    default -> {
                        progressBar.setStyle("-fx-accent: #1967D2;");
                        percentLabel.setTextFill(Color.valueOf("#1967D2"));
                        statusLabel.setTextFill(Color.valueOf("#5F6368"));
                        progressBar.setProgress(0.0);
                        percentLabel.setText("0%");
                    }
                }
            } else {
                nameLabel.setText("Unknown file");
                statusLabel.setText(task.getMessage() != null ? task.getMessage() : "Processing...");
                progressBar.setProgress(0.0);
                percentLabel.setText("0%");
            }
        }
    }
}