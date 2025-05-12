package com.ibrasoft.jdriveclonr.service.export;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;

public class DefaultExporter implements IDocumentExporter {
    @Override
    public void exportDocument(String filePath, ExportFormat format) {

    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return true;
    }
}
