package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.File;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.service.ServiceRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.Provider;
import java.time.Duration;
import java.util.Map;

public class ExportUtils {

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

    /**
     * Sets the last modified time of a file to match the given Google DateTime.
     *
     * @param filePath The file to update.
     * @param dateTime The Google API client DateTime.
     * @throws IOException If setting the file time fails.
     */
    public static void setLastModifiedFromDateTime(java.io.File filePath, DateTime dateTime) throws IOException {
        if (!filePath.exists()) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        Path path = filePath.toPath();
        FileTime fileTime = FileTime.fromMillis(dateTime.getValue());
        Files.setLastModifiedTime(path, fileTime);
    }

    public static void downloadNormally(DriveItem d,String filePath, ExportFormat mime) throws IOException {
        String fileID = d.getId();

//        try {
//            if (mime != ExportFormat.DEFAULT) {
//                try {
//                    ServiceRepository.getDriveService().files().export(fileID, mime.getMimeType())
//                            .executeMediaAndDownloadTo(target);
//                } catch (IOException e) {
//                    // We have likely run into a scenario where the file is too big to be exported, therefore we must
//                    // Use the export links trick.
//                    String downloadLink = fetchExportLinksFromFileId(fileID, mime);
//                    downloadFromExportLinkInto(ServiceRepository.getCredential().getAccessToken(), downloadLink, target);
//                }
//            } else {
//                try {
//                    this.driveService.files().get(fileID)
//                            .setSupportsAllDrives(true)
//                            .executeMediaAndDownloadTo(target);
//                } catch (com.google.api.client.http.HttpResponseException e) {
//                    // We have likely encountered a "schrodinger's file" scenario where the file is somehow both shared
//                    // and not shared with us => Use Binary Export Links trick
//
//                    String downloadLink = d.getBinaryURL();
//                    if (downloadLink != null) {
//                        downloadFromExportLinkInto(ServiceRepository.getCredential().getAccessToken(), downloadLink, target);
//                    } else {
//                        throw new IOException("Unable to download file: " + e.getMessage());
//                    }
//
//                }
//            }
//        } catch (GoogleJsonResponseException e) {
//            System.err.println("Unable to move file: " + e.getDetails());
//            throw e;
//        }
    }
}
