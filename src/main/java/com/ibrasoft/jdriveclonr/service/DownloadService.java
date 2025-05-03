package com.ibrasoft.jdriveclonr.service;

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
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Thin wrapper: splits files evenly into N worker tasks,
 * aggregates overall progress, supports cancel/shutdown.
 */
@Data
public class DownloadService {

    private DriveAPIService service;

    public void downloadFile(DriveItem root, ConfigModel config) throws IOException {
        // The destination directory will contain all the files - make a folder inside the destination directory
        // To
        File accDest = new File(config.getDestinationDirectory().toFile(), "DriveClonr");
        downloadFile(root, accDest, config);
    }

    private void downloadFile(DriveItem d, File currPath, ConfigModel config) throws IOException {
        // Before anything, check if the directory exists. If not, create it
        if (!currPath.exists()) {
            currPath.mkdirs();
        }
        // If this is a folder, create that folders directory and recurse on all the children
        if (d.isFolder()) {
            File folder = new File(currPath, d.getName());
            folder.mkdirs();
            for (DriveItem child : d.getChildren()) {
                downloadFile(child, folder, config);
            }
        } else {
            ByteArrayOutputStream stream = this.service.downloadFile(d.getId(), config.getExportFormat(d.getMimeType()));
            String newName = FileUtils.sanitizeFilename(d.getName());
            // If the file already exists, append a number to the end of the file
            int i = 1;
            while (new File(currPath, newName).exists()) {
                newName = FileUtils.sanitizeFilename(d.getName()) + "_" + i++;
            }
            // Write the file to the destination directory
            try (OutputStream out = new FileOutputStream(new File(currPath, newName))) {
                stream.writeTo(out);
            } catch (IOException e) {
                throw new IOException("Error writing file: " + d.getName(), e);
            }
        }
    }

}
