package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.model.GoogleMime;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Data
@NoArgsConstructor
public class GoogleSheetsExporter implements IDocumentExporter {

    final GoogleMime SUPPORTED_MIME = GoogleMime.SHEETS;

    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format) {
        if (!format.isPrimitive()) {
            // If not primitive => Export as normal
        }
    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals(this.SUPPORTED_MIME.getMimeType());
    }
}
