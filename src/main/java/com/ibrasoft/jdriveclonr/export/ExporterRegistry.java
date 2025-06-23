package com.ibrasoft.jdriveclonr.export;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;

import java.util.ArrayList;
import java.util.List;

public class ExporterRegistry {

    private final ThreadLocal<List<IDocumentExporter>> exporters = ThreadLocal.withInitial(() -> {
        List<IDocumentExporter> list = new ArrayList<>();
        list.add(new GoogleSheetsExporter());
        list.add(new GoogleSlidesExporter());
        list.add(new DefaultExporter());
        return list;
    });

    public IDocumentExporter find(DriveItem item, ExportFormat fmt) {
        if (!fmt.isPrimitive()){
            // return the last exporter in the list. i.e: the DefaultExporter
            return exporters.get().getLast();
        }
        return exporters.get().stream()
                .filter(e -> e.supports(item, fmt))
                .findFirst()
                .orElse(null);
    }
}
