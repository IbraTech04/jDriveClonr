package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.slides.v1.Slides;
import com.google.api.services.slides.v1.model.Page;
import com.google.api.services.slides.v1.model.Presentation;
import com.google.api.services.slides.v1.model.Thumbnail;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.model.GoogleMime;
import com.ibrasoft.jdriveclonr.service.ServiceRepository;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor
public class GoogleSlidesExporter implements IDocumentExporter {

    final GoogleMime SUPPORTED_MIME = GoogleMime.SLIDES;

    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format) throws IOException, InterruptedException {
        if (!format.isPrimitive()) {
            ExportUtils.downloadNormally(d, filePath, format);
        } else {
            // A) Create a folder with the current drive item name
            File dest = new File(filePath, d.getName());

            if (!dest.mkdir()) {
                throw new IOException("Failed to create directory: " + filePath + File.separator + d.getName());
            }

            try {
                Presentation presentation = ServiceRepository.getSlidesService()
                        .presentations()
                        .get(d.getId())
                        .execute();

                List<Page> slides = presentation.getSlides();

                for (int i = 0; i < slides.size(); i++) {
                    Page slide = slides.get(i);
                    String pageId = slide.getObjectId();

                    // Use Google Slides API to generate a PNG thumbnail
                    Thumbnail thumbnail = ServiceRepository.getSlidesService()
                            .presentations()
                            .pages()
                            .getThumbnail(d.getId(), pageId)
                            .setThumbnailPropertiesMimeType(format.getShortMime().toUpperCase(Locale.ROOT))
                            .setThumbnailPropertiesThumbnailSize("LARGE")
                            .execute();

                    String contentUrl = thumbnail.getContentUrl();

                    String slideName = String.format("Slide %02d", i + 1);
                    File outFile = new File(dest, slideName + ".png");

                    try (InputStream in = new URL(contentUrl).openStream();
                         FileOutputStream output = new FileOutputStream(outFile)) {
                        in.transferTo(output);
                    }

                    Thread.sleep(500); // optional throttle to be gentle on API
                }
            } catch (Exception e) {
                throw new IOException("Failed to export slides: " + e.getMessage(), e);
            }
        }
    }



    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals(this.SUPPORTED_MIME.getMimeType());
    }
}
