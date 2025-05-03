package com.ibrasoft.jdriveclonr.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;

@Data
public class ConfigModel {
    private Path destinationDirectory;
    private Map<String, String> exportFormats;
    
    public ConfigModel() {
        exportFormats = new HashMap<>();
        // Set default export formats for each Google Workspace type
        exportFormats.put(MimeTypeMapping.DOCS_MIME_TYPE, MimeTypeMapping.getDefaultExportMimeType(MimeTypeMapping.DOCS_MIME_TYPE));
        exportFormats.put(MimeTypeMapping.SHEETS_MIME_TYPE, MimeTypeMapping.getDefaultExportMimeType(MimeTypeMapping.SHEETS_MIME_TYPE));
        exportFormats.put(MimeTypeMapping.SLIDES_MIME_TYPE, MimeTypeMapping.getDefaultExportMimeType(MimeTypeMapping.SLIDES_MIME_TYPE));
        exportFormats.put(MimeTypeMapping.DRAWING_MIME_TYPE, MimeTypeMapping.getDefaultExportMimeType(MimeTypeMapping.DRAWING_MIME_TYPE));
        exportFormats.put(MimeTypeMapping.JAMBOARD_MIME_TYPE, MimeTypeMapping.getDefaultExportMimeType(MimeTypeMapping.JAMBOARD_MIME_TYPE));
    }
    
    public void setExportFormat(String googleMimeType, String uiValue) {
        String exportMimeType = MimeTypeMapping.getExportMimeType(uiValue);
        if (exportMimeType != null) {
            exportFormats.put(googleMimeType, exportMimeType);
        }
    }
    
    public String getExportFormat(String googleMimeType) {
        return exportFormats.get(googleMimeType);
    }
    
    public String getUiValueForType(String googleMimeType) {
        String mimeType = exportFormats.get(googleMimeType);
        return MimeTypeMapping.getUiValue(mimeType);
    }
}