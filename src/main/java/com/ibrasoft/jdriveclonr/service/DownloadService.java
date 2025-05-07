package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.util.DateTime;
import com.ibrasoft.jdriveclonr.model.ConfigModel;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Multithreaded download service that splits work across multiple worker threads,
 * aggregates overall progress, and supports cancel/shutdown.
 */
@Data
public class DownloadService {

    private DriveAPIService service;
    private Consumer<Double> progressCallback;
    private Consumer<String> messageCallback;
    private Consumer<Task<?>> newTaskCallback;
    private volatile boolean isCancelled = false;
    private final AtomicLong bytesProcessed = new AtomicLong(0);
    private long totalBytes = 0;

    // Thread pool for parallel downloads
    private ExecutorService executorService;
    private int maxThreads = Runtime.getRuntime().availableProcessors();
    private List<Future<?>> runningTasks = new ArrayList<>();

    public DownloadService() {
        // Initialize with default thread count based on available processors
        this.executorService = Executors.newFixedThreadPool(maxThreads);
    }

    public void setMaxThreads(int threads) {
        if (threads > 0) {
            this.maxThreads = threads;
            // Shutdown existing pool and create a new one
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            this.executorService = Executors.newFixedThreadPool(threads);
        }
    }

    public void downloadFile(DriveItem root, ConfigModel config, Consumer<Double> progressCallback,
                             Consumer<String> messageCallback, Consumer<Task<?>> newTaskCallback) throws IOException {
        this.progressCallback = progressCallback;
        this.messageCallback = messageCallback;
        this.newTaskCallback = newTaskCallback;
        this.isCancelled = false;
        this.bytesProcessed.set(0);
        this.runningTasks.clear();

        // Calculate total bytes first
        calculateTotalBytes(root);

        // Create a dummy directory to ensure everything is organized
        File accDest = new File(config.getDestinationDirectory().toFile(), "DriveClonr");
        if (!accDest.exists()) {
            if (!accDest.mkdirs()){
                throw new IOException("Failed to create directory: " + accDest.getAbsolutePath());
            }
        }

        // Start the recursive download
        try {
            downloadFileRecursive(root, accDest, config);

            // Wait for all tasks to complete
            for (Future<?> task : runningTasks) {
                try {
                    task.get();
                } catch (InterruptedException | ExecutionException e) {
                    if (!isCancelled) {
                        throw new IOException("Download error: " + e.getMessage(), e);
                    }
                }
            }
        } finally {
            // Clean up when done
            shutdownThreadPool();
        }
    }

    private void shutdownThreadPool() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void calculateTotalBytes(DriveItem item) {
        if (!item.isFolder()) {
            totalBytes += item.getSize();
        }
        for (DriveItem child : item.getChildren()) {
            calculateTotalBytes(child);
        }
    }

    private void downloadFileRecursive(DriveItem d, File currPath, ConfigModel config) throws IOException {
        if (isCancelled) {
            throw new IOException("Download cancelled");
        }

        // Before anything, check if the directory exists. If not, create it
        if (!currPath.exists()) {
            if (!currPath.mkdirs()) {
                throw new IOException("Failed to create directory: " + currPath.getAbsolutePath());
            }
        }

        // If this is a folder, create that folder's directory and recurse on all the children
        if (d.isFolder()) {
            updateMessage("Creating folder: " + d.getName());
            File folder = new File(currPath, FileUtils.sanitizeFilename(d.getName()));
            if (!folder.exists())
                if (!folder.mkdir())
                    throw new IOException("Failed to create directory: " + folder.getAbsolutePath());

            if (!d.isLoaded()) {
                d.clearChildren();
                d.setChildren(d.getNext().get());
            }

            for (DriveItem child : d.getChildren()) {
                downloadFileRecursive(child, folder, config);
            }
        } else {
            // For files, create and submit download tasks to the thread pool
            submitDownloadTask(d, currPath, config);
        }
    }

    private void submitDownloadTask(DriveItem item, File destinationDir, ConfigModel config) {
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Downloading: " + item.getName());

                try {
                    ByteArrayOutputStream stream = service.downloadFile(item.getId(), config.getExportFormat(item.getMimeType()));
                    String newName = FileUtils.sanitizeFilename(item.getName());

                    // If the file already exists, append a number to the end of the file
                    int i = 1;
                    while (new File(destinationDir, newName).exists()) {
                        String baseFileName = item.getName();
                        int dotIndex = baseFileName.lastIndexOf('.');
                        if (dotIndex > 0) {
                            baseFileName = baseFileName.substring(0, dotIndex);
                        }
                        baseFileName = FileUtils.sanitizeFilename(baseFileName);
                        newName = baseFileName + "_" + i++;
                    }

                    // Add the necessary extension to the file
                    String extension = config.getExportFormat(item.getMimeType()).getExtension();
                    newName += extension;

                    // Write the file to the destination directory
                    File outFile = new File(destinationDir, newName);
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        stream.writeTo(out);
                        updateProgress(item.getSize(), item.getSize());
                        setLastModifiedFromDateTime(outFile, item.getModifiedTime());
                    } catch (IOException e) {
                        throw new IOException("Error writing file: " + item.getName(), e);
                    }
                } catch (Exception e) {
                    if (!isCancelled) {
                        throw e;
                    }
                }
                return null;
            }
        };

        // Add progress and message listeners
        downloadTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.doubleValue() == 1.0) {
                // Update the global progress tracker when the file is complete
                updateProgress(item.getSize());
            }
        });

        // Notify UI of new task
        if (newTaskCallback != null) {
            Platform.runLater(() -> newTaskCallback.accept(downloadTask));
        }

        // Submit the task to the thread pool
        Future<?> future = executorService.submit(downloadTask);
        runningTasks.add(future);
    }

    /**
     * Sets the last modified time of a file to match the given Google DateTime.
     *
     * @param filePath The file to update.
     * @param dateTime The Google API client DateTime.
     * @throws IOException If setting the file time fails.
     */
    public static void setLastModifiedFromDateTime(File filePath, DateTime dateTime) throws IOException {
        if (!filePath.exists()) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        Path path = filePath.toPath();
        FileTime fileTime = FileTime.fromMillis(dateTime.getValue());
        Files.setLastModifiedTime(path, fileTime);
    }

    private void updateProgress(long bytes) {
        long processed = bytesProcessed.addAndGet(bytes);
        if (progressCallback != null && totalBytes > 0) {
            double progress = (double) processed / totalBytes;
            Platform.runLater(() -> progressCallback.accept(progress));
        }
    }

    private void updateMessage(String message) {
        if (messageCallback != null) {
            Platform.runLater(() -> messageCallback.accept(message));
        }
    }

    public void cancel() {
        this.isCancelled = true;

        // Cancel all running tasks
        for (Future<?> task : runningTasks) {
            task.cancel(true);
        }

        // Shutdown the thread pool
        shutdownThreadPool();
    }
}