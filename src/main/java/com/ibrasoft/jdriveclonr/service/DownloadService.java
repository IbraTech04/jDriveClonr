package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.util.DateTime;
import com.ibrasoft.jdriveclonr.model.ConfigModel;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private List<Future<?>> runningTasks = new ArrayList<>();

    // Use ConcurrentHashMap for tracking filename usage across threads
    private final ConcurrentHashMap<String, Set<String>> folderFileNamesMap = new ConcurrentHashMap<>();

    public final ThreadLocal<DriveAPIService> serviceThreadLocal = ThreadLocal.withInitial(() -> {
        try {
            return this.service.createCopy();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    });

    public DownloadService() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void downloadFile(DriveItem root, ConfigModel config, Consumer<Double> progressCallback,
                             Consumer<String> messageCallback, Consumer<Task<?>> newTaskCallback) throws IOException {
        this.progressCallback = progressCallback;
        this.messageCallback = messageCallback;
        this.newTaskCallback = newTaskCallback;
        this.isCancelled = false;
        this.bytesProcessed.set(0);
        this.runningTasks.clear();
        this.folderFileNamesMap.clear();

        // Calculate total bytes first
        calculateTotalBytes(root);

        String dateTime = String.format("%1$tY-%1$tm-%1$td %1$tH-%1$tM-%1$tS", System.currentTimeMillis());
        File accDest = new File(config.getDestinationDirectory().toFile(), "DriveClonr - " + dateTime);
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

    /**
     * Recursively downloads files and folders from Google Drive.
     * @param d The DriveItem to download
     * @param currPath The current directory to download into
     * @param config The application configuration
     * @throws IOException If an error occurs during download
     */
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
            String folderName = FileUtils.sanitizeFilename(d.getName());
            File folder = new File(currPath, folderName);
            if (!folder.exists()) {
                if (!folder.mkdir()) {
                    throw new IOException("Failed to create directory: " + folder.getAbsolutePath());
                }
            }

            // Initialize the folder path in our concurrent map
            String folderPathKey = folder.getAbsolutePath();
            folderFileNamesMap.putIfAbsent(folderPathKey, ConcurrentHashMap.newKeySet());

            if (!d.isLoaded()) {
                d.clearChildren();
                d.setChildren(d.getNext().get());
            }

            // Pre-process all child names before submission
            preProcessChildren(d, config, folderPathKey);

            for (DriveItem child : d.getChildren()) {
                downloadFileRecursive(child, folder, config);
            }
        } else {
            // For files, create and submit download tasks to the thread pool
            submitDownloadTask(d, currPath, config);
        }
    }

    /**
     * Pre-processes the children of a DriveItem to sanitize filenames and resolve conflicts.
     * @param parent The parent DriveItem
     * @param config The application configuration
     * @param folderPathKey The key for the folder path in the map
     */
    private void preProcessChildren(DriveItem parent, ConfigModel config, String folderPathKey) {
        if (!parent.isFolder()) return;

        // Get the set of existing names for this folder
        Set<String> existingNames = folderFileNamesMap.get(folderPathKey);

        // First, sanitize all filenames
        for (DriveItem child : parent.getChildren()) {
            String sanitizedName = FileUtils.sanitizeFilename(child.getName());
            child.setName(sanitizedName);
        }

        // Then resolve conflicts
        for (DriveItem child : parent.getChildren()) {
            String finalName = resolveFilenameConflict(child, config, folderPathKey);
            child.setName(finalName);

            // Mark this filename as used in this folder
            existingNames.add(finalName);
        }
    }

    /**
     * Submits a download task to the thread pool for the given DriveItem.
     * @param item The DriveItem to download
     * @param destinationDir The directory to download the file into
     * @param config The application configuration
     */
    private void submitDownloadTask(DriveItem item, File destinationDir, ConfigModel config) {
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Downloading: " + item.getName());

                try {
                    // Get the path key for this destination directory
                    String folderPathKey = destinationDir.getAbsolutePath();

                    // Ensure we have a filename that won't conflict (double-check before writing)
                    String finalName;
                    synchronized (folderFileNamesMap) {
                        finalName = resolveFilenameConflict(item, config, folderPathKey);
                        folderFileNamesMap.get(folderPathKey).add(finalName);
                    }

                    // Create the output file
                    File outFile = new File(destinationDir, finalName);

                    // Create a FileOutputStream to pass to DownloadService
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        serviceThreadLocal.get().downloadInto(
                                item.getId(),
                                config.getExportFormat(item.getMimeType()),
                                fos
                        );

                        // Set the last modified time
                        setLastModifiedFromDateTime(outFile, item.getModifiedTime());
                    }

                    updateProgress(item.getSize(), item.getSize());
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
     * Resolves filename conflicts by checking against existing names
     * and generating a new name if needed
     * @param item The DriveItem to resolve
     * @param config The application configuration
     * @param folderPathKey The key for the folder path in the map
     */
    private String resolveFilenameConflict(DriveItem item, ConfigModel config, String folderPathKey) {
        Set<String> existingNames = folderFileNamesMap.computeIfAbsent(
                folderPathKey, k -> ConcurrentHashMap.newKeySet()
        );

        String baseFileName = item.getName();
        int dotIndex = baseFileName.lastIndexOf('.');
        String nameWithoutExt = (dotIndex > 0) ? baseFileName.substring(0, dotIndex) : baseFileName;
        String originalExtension = (dotIndex > 0) ? baseFileName.substring(dotIndex) : "";

        // Get the appropriate file extension
        String extension = config.getExportFormat(item.getMimeType()).getExtension().isEmpty() ?
                originalExtension : config.getExportFormat(item.getMimeType()).getExtension();

        if (extension.isEmpty()) {
            extension = ExportFormat.getFileExtensionFromMimeType(item.getMimeType());
        }

        // Try the base name first
        String finalName = nameWithoutExt + extension;

        // If the name exists, start appending numbers
        int counter = 1;
        while (existingNames.contains(finalName) || new File(folderPathKey, finalName).exists()) {
            finalName = nameWithoutExt + "_" + counter + extension;
            counter++;
        }

        return finalName;
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

    /**
     * Updates the progress of the download.
     * @param bytes The number of bytes processed.
     */
    private void updateProgress(long bytes) {
        long processed = bytesProcessed.addAndGet(bytes);
        if (progressCallback != null && totalBytes > 0) {
            double progress = (double) processed / totalBytes;
            Platform.runLater(() -> progressCallback.accept(progress));
        }
    }

    /**
     * Updates the message displayed in the UI.
     * @param message The message to display.
     */
    private void updateMessage(String message) {
        if (messageCallback != null) {
            Platform.runLater(() -> messageCallback.accept(message));
        }
    }

    /**
     * Cancels the download process and shuts down the thread pool.
     */
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