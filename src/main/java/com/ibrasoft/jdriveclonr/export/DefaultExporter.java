package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.model.File;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.service.ServiceRepository;
import com.ibrasoft.jdriveclonr.utils.ProgressTrackingOutputStream;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Data
@NoArgsConstructor
public class DefaultExporter implements IDocumentExporter {
    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        // Return true because this is the "default" exporter - it supports everything
        return true;
    }

    public static String fetchExportLinksFromFileId(String fileId, ExportFormat mime) throws IOException {
        File file = ServiceRepository.getDriveService().files().get(fileId)
                .setFields("exportLinks")
                .execute();
        Map<String, String> exportLinks = file.getExportLinks();
        return exportLinks.get(mime.getMimeType());
    }

    public static void downloadFromExportLinkInto(String token,
                                                  String link,
                                                  OutputStream target) throws IOException, InterruptedException {
        ServiceRepository.getRateLimiter().acquire();
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(link))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            try (InputStream errStream = response.body()) {
                String err = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("Failed to download: HTTP " + response.statusCode() + " – " + err);
            }
        }

        try (InputStream in = response.body()) {
            in.transferTo(target);
        }
    }

    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat mime, ProgressCallback pc) throws IOException, InterruptedException {
        String fileID = d.getId();
        FileOutputStream target = new FileOutputStream(filePath + d.getName() + mime.getExtension());

        try {
            if (mime != ExportFormat.DEFAULT) {
                try {
                    ServiceRepository.getDriveService().files().export(fileID, mime.getMimeType())
                            .executeMediaAndDownloadTo(target);
                } catch (IOException e) {
                    // We have likely run into a scenario where the file is too big to be exported, therefore we must
                    // Use the export links trick.
                    String downloadLink = fetchExportLinksFromFileId(fileID, mime);
                    downloadFromExportLinkInto(ServiceRepository.getCredential().getAccessToken(), downloadLink, target);
                }
            } else {
                try {
                    ServiceRepository.getDriveService().files().get(fileID)
                            .setSupportsAllDrives(true)
                            .executeMediaAndDownloadTo(target);
                } catch (com.google.api.client.http.HttpResponseException e) {
                    // We have likely encountered a "schrodinger's file" scenario where the file is somehow both shared
                    // and not shared with us => Use Binary Export Links trick

                    String downloadLink = d.getBinaryURL();
                    if (downloadLink != null) {
                        downloadFromExportLinkInto(ServiceRepository.getCredential().getAccessToken(), downloadLink, target);
                    } else {
                        throw new IOException("Unable to download file: " + e.getMessage());
                    }

                }
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("Unable to move file: " + e.getDetails());
            throw e;
        }
    }
}
