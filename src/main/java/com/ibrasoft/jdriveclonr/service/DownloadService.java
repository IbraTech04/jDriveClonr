package com.ibrasoft.jdriveclonr.service;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.export.ExporterRegistry;
import com.ibrasoft.jdriveclonr.model.DriveDownloadTask;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.ui.AuthController;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Data;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multithreaded download service that splits work across multiple worker threads,
 * aggregates overall progress, and supports cancel/shutdown.
 */
@Data
public class DownloadService extends Service<Void> {

    private DriveItem rootItem;
    private final ExecutorService executorService;    private final ObservableList<Task<?>> downloadTasks = FXCollections.observableArrayList();
    private final ObservableList<Task<?>> completedTasks = FXCollections.observableArrayList();
    private final ObservableList<Task<?>> failedTasks = FXCollections.observableArrayList();
    private ExporterRegistry exporterRegistry;

    public DownloadService(DriveItem rootItem) {
        this.rootItem = rootItem;
        this.executorService = Executors.newFixedThreadPool(
                App.getConfigModel().getThreadCount() > 0 ? App.getConfigModel().getThreadCount() : 4);
        
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

        @Override
        protected Void call() throws Exception {
            Platform.runLater(downloadTasks::clear);
            recurseAndAddTasks(rootItem, App.getConfigModel().getDestinationDirectory());
            return null;
        }

        public void recurseAndAddTasks(DriveItem root, Path currPath) {
            if (root.isFolder()) {
                // Create directory if it doesn't exist
                if (!currPath.toFile().exists()) {
                    System.out.println("Creating directory: " + currPath);
                    if (!currPath.toFile().mkdirs()) {
                        System.err.println("Failed to create directory: " + currPath);
                        return;
                    }
                }
                
                if (!root.isLoaded()) {
                    try {
                        root.loadChildren();
                        System.out.println("Loaded " + (root.getChildren() != null ? root.getChildren().size() : 0) 
                                         + " children for folder: " + root.getName());
                    } catch (Exception e) {
                        System.err.println("Failed to load children for folder '" + root.getName() + "': " + e.getMessage());
                        e.printStackTrace();
                        return;
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
                            else
                                recurseAndAddTasks(child, currPath);
                        } catch (Exception e) {
                            System.err.println("Error processing child '" + child.getName() + "': " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("Folder '" + root.getName() + "' has no children or children failed to load");
                }
            } else {
                try {
                    DriveDownloadTask task = new DriveDownloadTask(root, currPath.toString(), exporterRegistry);

                    Platform.runLater(() -> downloadTasks.add(task));

                    task.setOnSucceeded(event -> Platform.runLater(() -> completedTasks.add(task)));
                    task.setOnFailed(event -> Platform.runLater(() -> failedTasks.add(task)));

                    executorService.submit(task);

                    System.out.println("Started download task for: " + root.getName() + " at " + currPath + " for file type: " + root.getMimeType());
                } catch (Exception e) {
                    System.err.println("Failed to create download task for '" + root.getName() + "': " + e.getMessage());
                }
            }
        }
    }
}