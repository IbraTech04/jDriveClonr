package com.ibrasoft.jdriveclonr.service.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.slides.v1.Slides;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleSlidesExporter implements IDocumentExporter {

    private Slides service;

    private Credential creds;
    public GoogleSlidesExporter(Credential creds) throws GeneralSecurityException, IOException {
        this.creds = creds;
        this.service = new Slides.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), creds)
                .setApplicationName("DriveClonr")
                .build();
    }
    @Override
    public void exportDocument(String filePath, ExportFormat format) {
    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals("application/vnd.google-apps.presentation");
    }
}
