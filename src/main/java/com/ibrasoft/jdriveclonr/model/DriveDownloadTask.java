package com.ibrasoft.jdriveclonr.model;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.export.ExporterRegistry;
import com.ibrasoft.jdriveclonr.export.IDocumentExporter;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import com.ibrasoft.jdriveclonr.utils.ProgressTrackingOutputStream;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.FileOutputStream;

@AllArgsConstructor
@Data
public class DriveDownloadTask extends Task<Void> {

    private DriveItem driveItem;
    private String destinationPath;
    private ExporterRegistry exporterRegistry;

    @Override
    protected Void call() throws Exception {
        IDocumentExporter exporter = this.exporterRegistry.find(driveItem, App.getConfigModel().getExportFormat(driveItem.getMimeType()));

        exporter.exportDocument(driveItem, destinationPath,
                App.getConfigModel().getExportFormat(driveItem.getMimeType()), (workDone, totalWork, message) -> {
                    updateProgress(workDone, totalWork);
                    updateMessage(message);
                });


        return null;
    }
}
