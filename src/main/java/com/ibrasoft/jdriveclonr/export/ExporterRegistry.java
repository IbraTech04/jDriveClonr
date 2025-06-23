package com.ibrasoft.jdriveclonr.export;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.service.GoogleServiceFactory;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ExporterRegistry {

    private final List<IDocumentExporter> exporters;    public static ExporterRegistry create(GoogleServiceFactory.GoogleServices services) {
        List<IDocumentExporter> exporters = new ArrayList<>();
        
        // Add specialized exporters with dependency injection
        exporters.add(new GoogleSheetsExporter(services.getSheetsService(), services.getCredential()));
        exporters.add(new GoogleSlidesExporter(services.getSlidesService(), services.getCredential()));
        
        // Add default exporter with dependency injection
        exporters.add(new DefaultExporter(services.getDriveService(), services.getCredential(), services.getRateLimiter()));
        
        return new ExporterRegistry(exporters);
    }

    public IDocumentExporter find(DriveItem item, ExportFormat fmt) {
        if (!fmt.isPrimitive()){
            // return the last exporter in the list. i.e: the DefaultExporter
            return exporters.getLast();
        }
        return exporters.stream()
                .filter(e -> e.supports(item, fmt))
                .findFirst()
                .orElse(null);
    }
}
