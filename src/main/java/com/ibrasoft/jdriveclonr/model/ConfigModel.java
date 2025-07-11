package com.ibrasoft.jdriveclonr.model;

import com.ibrasoft.jdriveclonr.model.mime.ExportFormat;
import com.ibrasoft.jdriveclonr.model.mime.GoogleMime;
import lombok.Data;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ConfigModel {
    private static final Logger logger = LoggerFactory.getLogger(ConfigModel.class);
    private Path destinationDirectory;
    private final Map<String, ExportFormat> exportFormats;
    private int threadCount = 4; // Default thread count

    public ConfigModel() {
        logger.info("Initializing ConfigModel");
        exportFormats = new HashMap<>();
        exportFormats.put(GoogleMime.DOCS.getMimeType(), ExportFormat.DOCX);
        exportFormats.put(GoogleMime.SHEETS.getMimeType(), ExportFormat.XLSX);
        exportFormats.put(GoogleMime.SLIDES.getMimeType(), ExportFormat.PPTX);
        exportFormats.put(GoogleMime.DRAWINGS.getMimeType(), ExportFormat.PNG);
        exportFormats.put(GoogleMime.JAMBOARD.getMimeType(), ExportFormat.PDF);
    }

    public void setExportFormat(String googleMimeType, ExportFormat format) {
        exportFormats.put(googleMimeType, format);
    }

    public ExportFormat getExportFormat(String googleMimeType) {
        return exportFormats.getOrDefault(googleMimeType, ExportFormat.DEFAULT);
    }

    public String getUiValueForType(String googleMimeType) {
        ExportFormat format = exportFormats.get(googleMimeType);
        return format != null ? format.getUiLabel() : null;
    }
}