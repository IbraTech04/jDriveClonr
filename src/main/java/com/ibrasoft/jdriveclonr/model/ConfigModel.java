package com.ibrasoft.jdriveclonr.model;

import lombok.Data;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Data
public class ConfigModel {
    private Path destinationDirectory;
    private final Map<String, ExportFormat> exportFormats;

    public ConfigModel() {
        exportFormats = new HashMap<>();
        exportFormats.put(GoogleMime.DOCS, ExportFormat.DOCX);
        exportFormats.put(GoogleMime.SHEETS, ExportFormat.XLSX);
        exportFormats.put(GoogleMime.SLIDES, ExportFormat.PPTX);
        exportFormats.put(GoogleMime.DRAWINGS, ExportFormat.PNG);
        exportFormats.put(GoogleMime.JAMBOARD, ExportFormat.PDF);
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