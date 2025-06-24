package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.slides.v1.Slides;
import com.google.api.services.slides.v1.model.Page;
import com.google.api.services.slides.v1.model.Presentation;
import com.google.api.services.slides.v1.model.Thumbnail;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.model.GoogleMime;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;

@Data
@AllArgsConstructor
public class GoogleSlidesExporter implements IDocumentExporter {

    private final Slides slidesService;
    private final Credential credential;
    final GoogleMime SUPPORTED_MIME = GoogleMime.SLIDES;

    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format, ProgressCallback pc) throws IOException {
        // A) Create a folder with the current drive item name
        String sanitizedName = FileUtils.sanitizeFilename(d.getName() + " - " + d.getId().substring(0, 8));
        File dest = new File(filePath, sanitizedName);

        if (!dest.mkdir()) {
            throw new IOException("Failed to create directory: " + filePath + File.separator + sanitizedName);
        }
        try {
            Presentation presentation = slidesService
                    .presentations()
                    .get(d.getId())
                    .execute();

            List<Page> slides = presentation.getSlides();

            for (int i = 0; i < slides.size(); i++) {
                Page slide = slides.get(i);
                String pageId = slide.getObjectId();
                pc.updateProgress((i / (1.0 * slides.size())), 1.0, "Exporting slide: " + slide.getPageElements().getFirst().getObjectId());
                // Use Google Slides API to generate a PNG thumbnail
                Thumbnail thumbnail = slidesService
                        .presentations()
                        .pages()
                        .getThumbnail(d.getId(), pageId)
                        .setThumbnailPropertiesMimeType(format.getShortMime().toUpperCase(Locale.ROOT))
                        .setThumbnailPropertiesThumbnailSize("LARGE")
                        .execute();

                String contentUrl = thumbnail.getContentUrl();

                String slideName = String.format("Slide %02d", i + 1);
                File outFile = new File(dest, slideName + ".png");
                try (InputStream in = URI.create(contentUrl).toURL().openStream();
                     FileOutputStream output = new FileOutputStream(outFile)) {
                    in.transferTo(output);
                }

                pc.updateProgress((i + 1 / (1.0 * slides.size())), 1.0, "Exporting slide: " + slide.getPageElements().getFirst().getObjectId());
            }
        } catch (Exception e) {
            throw new IOException("Failed to export slides: " + e.getMessage(), e);
        }
    }


    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals(this.SUPPORTED_MIME.getMimeType());
    }
}
