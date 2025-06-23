package com.ibrasoft.jdriveclonr.model;

import com.ibrasoft.jdriveclonr.export.ExporterRegistry;
import com.ibrasoft.jdriveclonr.export.IDocumentExporter;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DriveDownloadTask extends Task<Void> {

    private DriveItem driveItem;
    private String destinationPath;

    @Override
    protected Void call() throws Exception {
        System.out.println(" \"Downloading\"" + driveItem.getName() + " to " + destinationPath);
        return null;
    }
}
