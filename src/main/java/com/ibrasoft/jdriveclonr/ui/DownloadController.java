package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.model.DriveDownloadTask;
import com.ibrasoft.jdriveclonr.model.DriveItem;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for the download view that manages the UI for downloading files from Google Drive.
 * Provides real-time progress updates, file-specific progress tracking, and download management.
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
    private Button cancelBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private StackPane emptyStatePane;
    @FXML
    private HBox failedHeaderBox;
    @FXML
    private VBox failedDownloadsContainer;
    @FXML
    private StackPane failedDownloadsPane;


    // Services and tasks
    private final DownloadService service;

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
        this.service = new DownloadService(DriveContentController.getSelectedRoot());
//        this.service.setService(App.getDriveService());
        this.filteredActiveTasks = new FilteredList<>(activeTasks, task -> task.getState() != Worker.State.SUCCEEDED && task.getState() != Worker.State.CANCELLED);
    }

    @Override
    public void initialize(URL u, ResourceBundle rb) {
        Bindings.bindContent(activeTasks, service.getDownloadTasks());
//        Bindings.bindContent(failed)
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
        failedCountLabel.setText("0 files");
        overallLabel.textProperty().bind(statusMessage);

        // Style the progress bar
        overallBar.setStyle("-fx-accent: #1967D2;");

        // Empty state visibility binding
        emptyStatePane.visibleProperty().bind(Bindings.isEmpty(activeTasks));
        threadList.visibleProperty().bind(Bindings.isNotEmpty(activeTasks));

        // Initialize failed downloads section (hidden initially)
        failedHeaderBox.setVisible(false);
        failedDownloadsPane.setVisible(false);
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
        this.service.start();
    }
//
//    private void addFailedFileToUI(FileFailure failure) {
//        // Make fail section visible if this is the first failure
//        if (!failedHeaderBox.isVisible()) {
//            failedHeaderBox.setVisible(true);
//            failedDownloadsPane.setVisible(true);
//        }
//
//        // Update the failed count label
//        failedCountLabel.setText(failedCount + (failedCount == 1 ? " file" : " files"));
//
//        // Create a row for this failed download
//        HBox failedItem = new HBox(10);
//        failedItem.setPadding(new Insets(8, 10, 8, 10));
//        failedItem.setStyle("-fx-background-color: #FDEDED; -fx-background-radius: 6;");
//
//        // File name label
//        Label nameLabel = new Label(failure.getFileName());
//        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D93025;");
//
//        // Error message button
//        Button detailsBtn = new Button("Details");
//        detailsBtn.setStyle(
//                "-fx-background-color: transparent; -fx-text-fill: #1967D2; " +
//                        "-fx-border-color: #1967D2; -fx-border-radius: 4; -fx-cursor: hand;"
//        );
//        detailsBtn.setOnAction(e -> showErrorDetails(failure));
//
//        // Add a spacer
//        Region spacer = new Region();
//        HBox.setHgrow(spacer, Priority.ALWAYS);
//
//        // Add components to the row
//        failedItem.getChildren().addAll(nameLabel, spacer, detailsBtn);
//
//        // Add to the container
//        failedDownloadsContainer.getChildren().add(failedItem);
//    }

    private void showErrorDetails(FileFailure failure) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Download Error Details");
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
//            int failedCount = failedFiles.size();
            int failedCount = 0;
            int successCount = count - failedCount;

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
                        successCount, successCount == 1 ? "file" : "files", timeText
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

        int successCount = totalCount - failedCount;
        String contentText = String.format(
                "Successfully cloned %d of %d files from Google Drive in %s.\n%d files failed to download.",
                successCount, totalCount, timeText, failedCount
        );
        alert.setContentText(contentText);

        // Create expandable content with list of failed files
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);

        StringBuilder failuresText = new StringBuilder();
        int index = 1;
//        for (FileFailure failure : failedFiles) {
//            failuresText.append(index++).append(". ")
//                    .append(failure.getFileName())
//                    .append(" - ")
//                    .append(failure.getErrorMessage())
//                    .append("\n\n");
//        }
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
//        if (downloadTask != null && !downloadTask.isDone()) {
//            service.cancel();
//            downloadTask.cancel();
//            statusMessage.set("Download cancelled");
//            closeBtn.setDisable(false);
//            cancelBtn.setDisable(true);
//        }
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
//        if (downloadTask != null && !downloadTask.isDone()) {
//            service.cancel();
//            downloadTask.cancel();
//        }
        cleanupTasks();

        // Don't remove failed files from display when cleanup is called
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
            bar.progressProperty().unbind();
            bar.progressProperty().bind(task.progressProperty());

            if (task instanceof DriveDownloadTask fileTask) {
                DriveItem item = fileTask.getDriveItem();
                nameLabel.setText(item.getName());
                statusLabel.setText(task.getMessage());
            }

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