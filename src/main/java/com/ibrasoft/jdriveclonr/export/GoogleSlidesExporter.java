package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.slides.v1.Slides;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.model.GoogleMime;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.security.GeneralSecurityException;

@NoArgsConstructor
public class GoogleSlidesExporter implements IDocumentExporter {

    final GoogleMime SUPPORTED_MIME = GoogleMime.SLIDES;

    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format) {
    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals(this.SUPPORTED_MIME.getMimeType());
    }
}
