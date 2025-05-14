package com.ibrasoft.jdriveclonr.export;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.model.GoogleMime;

public interface IDocumentExporter {

    final GoogleMime SUPPORTED_MIME = null;

    /**
     * Exports a document to the specified file path.
     *
     * @param filePath the path to export the document to
     * @param format the MIMEType to export the document in
     */
    void exportDocument(DriveItem d, String filePath, ExportFormat format);

    /**
     * Checks if the exporter supports the given DriveItem and ExportFormat.
     * @param d the DriveItem to check
     * @param format the ExportFormat to check
     * @return True if this exporter supports the given DriveItem and ExportFormat, false otherwise
     */
    boolean supports(DriveItem d, ExportFormat format);
}
