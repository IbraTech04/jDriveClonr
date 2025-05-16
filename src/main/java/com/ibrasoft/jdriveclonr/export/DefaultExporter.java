package com.ibrasoft.jdriveclonr.export;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class DefaultExporter implements IDocumentExporter {
    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format) throws IOException {
        ExportUtils.downloadNormally(d, filePath, format);
    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        // Return true because this is the "default" exporter - it supports everything 
        return true;
    }
}
