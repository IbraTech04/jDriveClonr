package com.ibrasoft.jdriveclonr.service.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import lombok.Data;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Data
public class GoogleSheetsExporter implements IDocumentExporter {

    private Sheets service;
    Credential creds;

    public GoogleSheetsExporter(Credential creds) throws GeneralSecurityException, IOException {
        this.creds = creds;
        this.service = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), creds)
                .setApplicationName("DriveClonr")
                .build();
    }
    @Override
    public void exportDocument(String filePath, ExportFormat format) {
    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals("application/vnd.google-apps.spreadsheet");
    }
}
