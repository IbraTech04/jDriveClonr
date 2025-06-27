package com.ibrasoft.jdriveclonr.service;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.export.ExporterRegistry;
import com.ibrasoft.jdriveclonr.model.DriveDownloadTask;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Multithreaded download service that splits work across multiple worker threads,
 * aggregates overall progress, and supports cancel/shutdown.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DownloadService extends Service<Void> {

    private final ExecutorService executorService;
    private final ObservableList<Task<?>> downloadTasks = FXCollections.observableArrayList();
    private final ObservableList<Task<?>> completedTasks = FXCollections.observableArrayList();
    private final ObservableList<Task<?>> failedTasks = FXCollections.observableArrayList();
    private DriveItem rootItem;
    private ExporterRegistry exporterRegistry;

    public DownloadService(DriveItem rootItem) {
        this.rootItem = rootItem;
        ThreadFactory daemonThreadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        };

        this.executorService = Executors.newFixedThreadPool(App.getConfigModel().getThreadCount() > 0 ? App.getConfigModel().getThreadCount() : 4, daemonThreadFactory);

        // Initialize services and exporter registry
        try {
            GoogleServiceFactory.GoogleServices services = GoogleServiceFactory.createServices(ServiceRepository.getCredential());
            this.exporterRegistry = ExporterRegistry.create(services);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize services for download", e);
        }
    }

    @Override
    protected Task<Void> createTask() {
        return new DriveDownloadCoordinatorTask();
    }

    private class DriveDownloadCoordinatorTask extends Task<Void> {
        private final List<Future<?>> submittedTasks = new ArrayList<>();

        @Override
        protected Void call() throws Exception {
            Platform.runLater(downloadTasks::clear);

            // First, count all the tasks that will be created
            int totalTaskCount = countTasks(rootItem);

            if (totalTaskCount == 0) {
                updateMessage("No files to download");
                return null;
            }

            updateMessage("Starting download of " + totalTaskCount + " files...");

            // Create and submit all download tasks
            recurseAndAddTasks(rootItem, App.getConfigModel().getDestinationDirectory());

            // Wait for all tasks to complete
            try {
                updateMessage("Waiting for all downloads to complete...");
                for (Future<?> future : submittedTasks) {
                    if (isCancelled()) {
                        future.cancel(true);
                        break;
                    }
                    try {
                        future.get(); // Wait for this task to complete
                    } catch (Exception e) {
                        // Task failed, but we continue waiting for others
                        System.err.println("Download task failed: " + e.getMessage());
                    }
                }
                updateMessage("All downloads completed");
            } catch (Exception e) {
                updateMessage("Download process encountered an error: " + e.getMessage());
                throw e;
            }

            return null;
        }

        private int countTasks(DriveItem root) {
            int count = 0;

            if (root.isFolder()) {
                if (!root.isLoaded()) {
                    try {
                        root.loadChildren();
                    } catch (Exception e) {
                        System.err.println("Failed to load children for counting in folder '" + root.getName() + "': " + e.getMessage());
                        return 0;
                    }
                }

                if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                    for (DriveItem child : root.getChildren()) {
                        if (child == null) continue;

                        String childId = child.getId();
                        if ("loading".equals(childId) || "empty".equals(childId) || "error".equals(childId)) {
                            continue;
                        }

                        try {
                            count += countTasks(child);
                        } catch (Exception e) {
                            System.err.println("Error counting tasks for child '" + child.getName() + "': " + e.getMessage());
                        }
                    }
                }
            } else {
                // This is a file, so it will create one download task
                count = 1;
            }

            return count;
        }

        public void recurseAndAddTasks(DriveItem root, Path currPath) {
            if (root.isFolder()) {
                // Create directory if it doesn't exist
                if (!currPath.toFile().exists()) {
                    if (!currPath.toFile().mkdirs()) {
                        throw new RuntimeException("Failed to create directory: " + currPath);
                    }
                }

                if (!root.isLoaded()) {
                    try {
                        root.loadChildren();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load children for folder '" + root.getName() + "': " + e.getMessage(), e);
                    }
                }

                if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                    for (DriveItem child : root.getChildren()) {
                        if (child == null) continue;

                        String childId = child.getId();
                        if ("loading".equals(childId) || "empty".equals(childId) || "error".equals(childId)) {
                            continue;
                        }

                        try {
                            if (child.isFolder())
                                recurseAndAddTasks(child, currPath.resolve(FileUtils.sanitizeFilename(child.getName())));
                            else recurseAndAddTasks(child, currPath);
                        } catch (Exception e) {
                            System.err.println("Error processing child '" + child.getName() + "': " + e.getMessage());
                        }
                    }
                }
            } else {
                try {
                    DriveDownloadTask task = new DriveDownloadTask(root, currPath.toString(), exporterRegistry);

                    Platform.runLater(() -> downloadTasks.add(task));

                    task.setOnSucceeded(event -> Platform.runLater(() -> {
                        downloadTasks.remove(task);
                        completedTasks.add(task);
                    }));
                    task.setOnFailed(event -> Platform.runLater(() -> {
                        downloadTasks.remove(task);
                        failedTasks.add(task);
                    }));

                    // Submit the task and track the Future
                    Future<?> future = executorService.submit(task);
                    submittedTasks.add(future);

                } catch (Exception e) {
                    System.err.println("Failed to create download task for '" + root.getName() + "': " + e.getMessage());
                }
            }
        }
    }
}