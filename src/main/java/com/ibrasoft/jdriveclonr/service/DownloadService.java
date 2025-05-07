package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.util.DateTime;
import com.ibrasoft.jdriveclonr.model.ConfigModel;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import lombok.Data;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Thin wrapper: splits files evenly into N worker tasks,
 * aggregates overall progress, supports cancel/shutdown.
 */
@Data
public class DownloadService {

    private DriveAPIService service;
    private Consumer<Double> progressCallback;
    private Consumer<String> messageCallback;
    private volatile boolean isCancelled = false;
    private final AtomicLong bytesProcessed = new AtomicLong(0);
    private long totalBytes = 0;

    public void downloadFile(DriveItem root, ConfigModel config, Consumer<Double> progressCallback, Consumer<String> messageCallback) throws IOException {
        this.progressCallback = progressCallback;
        this.messageCallback = messageCallback;
        this.isCancelled = false;
        this.bytesProcessed.set(0);
        
        // Calculate total bytes first
        calculateTotalBytes(root);
        
        // Create a dummy directory to ensure everything is organized
        File accDest = new File(config.getDestinationDirectory().toFile(), "DriveClonr");
        downloadFile(root, accDest, config);
    }

    private void calculateTotalBytes(DriveItem item) {
        if (!item.isFolder()) {
            totalBytes += item.getSize();
        }
        for (DriveItem child : item.getChildren()) {
            calculateTotalBytes(child);
        }
    }

    private void downloadFile(DriveItem d, File currPath, ConfigModel config) throws IOException {
        if (isCancelled) {
            throw new IOException("Download cancelled");
        }

        // Before anything, check if the directory exists. If not, create it
        if (!currPath.exists()) {
            currPath.mkdirs();
        }

        // If this is a folder, create that folder's directory and recurse on all the children
        if (d.isFolder()) {
            updateMessage("Creating folder: " + d.getName());
            File folder = new File(currPath, FileUtils.sanitizeFilename(d.getName()));
            folder.mkdirs();
            this.setLastModifiedFromDateTime(folder.getAbsoluteFile(), d.getModifiedTime());
            if (!d.isLoaded()){
                d.clearChildren();
                d.setChildren(d.getNext().get());
            }
            for (DriveItem child : d.getChildren()) {
                downloadFile(child, folder, config);
            }
        } else {
            updateMessage("Downloading: " + d.getName());
            ByteArrayOutputStream stream = this.service.downloadFile(d.getId(), config.getExportFormat(d.getMimeType()));
            String newName = FileUtils.sanitizeFilename(d.getName());
            
            // If the file already exists, append a number to the end of the file
            int i = 1;
            while (new File(currPath, newName).exists()) {
                String baseFileName = d.getName();
                int dotIndex = baseFileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseFileName = baseFileName.substring(0, dotIndex);
                }
                baseFileName = FileUtils.sanitizeFilename(baseFileName);
                newName = baseFileName + "_" + i++;
            }

            // Add the necessary extension to the file
            String extension = config.getExportFormat(d.getMimeType()).getExtension();
            newName += extension;

            // Write the file to the destination directory
            try (OutputStream out = new FileOutputStream(new File(currPath, newName))) {
                stream.writeTo(out);
                updateProgress(d.getSize());
                setLastModifiedFromDateTime(new File(currPath, newName), d.getModifiedTime());
            } catch (IOException e) {
                throw new IOException("Error writing file: " + d.getName(), e);
            }
        }
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
            progressCallback.accept(progress);
        }
    }

    private void updateMessage(String message) {
        if (messageCallback != null) {
            messageCallback.accept(message);
        }
    }

    public void cancel() {
        this.isCancelled = true;
    }
}
