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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multithreaded download service that discovers and downloads files concurrently,
 * with dynamic progress tracking and support for cancel/shutdown.
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
        private final AtomicInteger totalTasksDiscovered = new AtomicInteger(0);
        private final AtomicInteger totalTasksCompleted = new AtomicInteger(0);
        private volatile boolean discoveryComplete = false;

        @Override
        protected Void call() throws Exception {
            Platform.runLater(downloadTasks::clear);

            updateMessage("Starting download discovery and processing...");

            // Start discovery and downloading concurrently
            try {
                recurseAndAddTasks(rootItem, App.getConfigModel().getDestinationDirectory());
                discoveryComplete = true;
                
                updateMessage("Discovery complete. Waiting for remaining downloads...");

                // Wait for all submitted tasks to complete
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

                int completed = totalTasksCompleted.get();
                int discovered = totalTasksDiscovered.get();
                
                if (discovered == 0) {
                    updateMessage("No files found to download");
                } else {
                    updateMessage("Download complete: " + completed + "/" + discovered + " files processed");
                }

            } catch (Exception e) {
                updateMessage("Download process encountered an error: " + e.getMessage());
                throw e;
            }

            return null;
        }

        public void recurseAndAddTasks(DriveItem root, Path currPath) {
            if (isCancelled()) {
                return;
            }

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
                        if (child == null || isCancelled()) continue;

                        String childId = child.getId();
                        if ("loading".equals(childId) || "empty".equals(childId) || "error".equals(childId)) {
                            continue;
                        }

                        try {
                            if (child.isFolder()) {
                                recurseAndAddTasks(child, currPath.resolve(FileUtils.sanitizeFilename(child.getName())));
                            } else {
                                recurseAndAddTasks(child, currPath);
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing child '" + child.getName() + "': " + e.getMessage());
                        }
                    }
                }
            } else {
                // This is a file - increment discovered count and create download task
                int discovered = totalTasksDiscovered.incrementAndGet();
                
                try {
                    DriveDownloadTask task = new DriveDownloadTask(root, currPath.toString(), exporterRegistry);

                    Platform.runLater(() -> downloadTasks.add(task));

                    task.setOnSucceeded(event -> {
                        int completed = totalTasksCompleted.incrementAndGet();
                        Platform.runLater(() -> {
                            downloadTasks.remove(task);
                            completedTasks.add(task);
                        });
                        updateProgressMessage(completed, discovered);
                    });
                    
                    task.setOnFailed(event -> {
                        int completed = totalTasksCompleted.incrementAndGet();
                        Platform.runLater(() -> {
                            downloadTasks.remove(task);
                            failedTasks.add(task);
                        });
                        updateProgressMessage(completed, discovered);
                    });

                    Future<?> future = executorService.submit(task);
                    submittedTasks.add(future);

                    updateProgressMessage(totalTasksCompleted.get(), discovered);

                } catch (Exception e) {
                    System.err.println("Failed to create download task for '" + root.getName() + "': " + e.getMessage());
                }
            }
        }

        private void updateProgressMessage(int completed, int discovered) {
            if (discoveryComplete) {
                updateMessage("Downloading: " + completed + "/" + discovered + " files completed");
            } else {
                updateMessage("Discovering and downloading: " + completed + "/" + discovered + " files (discovery ongoing...)");
            }
        }
    }
}