package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.common.util.concurrent.RateLimiter;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import com.ibrasoft.jdriveclonr.utils.ProgressTrackingOutputStream;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@NoArgsConstructor
public class DefaultExporter implements IDocumentExporter {
    
    private Drive driveService;
    private Credential credential;
    private RateLimiter rateLimiter;    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        // Return true because this is the "default" exporter - it supports everything
        return true;
    }

    public String fetchExportLinksFromFileId(String fileId, ExportFormat mime) throws IOException {
        if (driveService == null) {
            throw new IllegalStateException("Drive service not initialized");
        }
        File file = driveService.files().get(fileId)
                .setFields("exportLinks")
                .execute();
        Map<String, String> exportLinks = file.getExportLinks();
        return exportLinks.get(mime.getMimeType());
    }

    public void downloadFromExportLinkInto(String token,
                                          String link,
                                          OutputStream target,
                                          ProgressCallback pc,
                                          String fileName) throws IOException, InterruptedException {
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }
        
        pc.updateProgress(0.0, 1.0, "Starting download: " + fileName);
        
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

        pc.updateProgress(0.1, 1.0, "Downloading: " + fileName);
        
        // Get content length for progress tracking
        long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1);
          try (InputStream in = response.body()) {
            if (contentLength > 0) {
                // Use progress tracking output stream if we know the size
                long[] bytesWritten = {0};
                try (ProgressTrackingOutputStream progressOut = new ProgressTrackingOutputStream(target, 
                        (bytes) -> {
                            bytesWritten[0] += bytes;
                            double progress = 0.1 + (0.8 * bytesWritten[0] / (double) contentLength);
                            long percent = bytesWritten[0] * 100 / contentLength;
                            pc.updateProgress(progress, 1.0, "Downloading: " + fileName + " (" + percent + "%)");
                        })) {
                    in.transferTo(progressOut);
                }
            } else {
                // Fallback without progress tracking
                in.transferTo(target);
            }
        }
        
        pc.updateProgress(1.0, 1.0, "Downloaded: " + fileName);
    }    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat mime, ProgressCallback pc) throws IOException, InterruptedException {
        if (driveService == null || credential == null) {
            throw new IllegalStateException("DefaultExporter not properly initialized with dependencies");
        }
          String fileID = d.getId();
        String sanitizedName = FileUtils.sanitizeFilename(d.getName());
        String fileName = sanitizedName + mime.getExtension();
        
        // Ensure proper path separator - filePath should end with separator
        String normalizedPath = filePath;
        if (!normalizedPath.endsWith(java.io.File.separator)) {
            normalizedPath += java.io.File.separator;
        }
        String fullPath = normalizedPath + fileName;
        
        // Debug logging
        System.out.println("DEBUG: DefaultExporter.exportDocument called with:");
        System.out.println("  - DriveItem: " + d.getName() + " (ID: " + d.getId() + ")");
        System.out.println("  - FilePath: '" + filePath + "'");
        System.out.println("  - Format: " + mime);
        System.out.println("  - Full path will be: '" + fullPath + "'");
        
        pc.updateProgress(0.0, 1.0, "Preparing to download: " + fileName);
        
        try (FileOutputStream target = new FileOutputStream(fullPath)) {
            if (mime != ExportFormat.DEFAULT) {
                pc.updateProgress(0.1, 1.0, "Exporting: " + fileName);
                try {
                    // Try direct export first
                    driveService.files().export(fileID, mime.getMimeType())
                            .executeMediaAndDownloadTo(target);
                    pc.updateProgress(1.0, 1.0, "Exported: " + fileName);
                } catch (IOException e) {
                    // File too big for direct export, use export links trick
                    pc.updateProgress(0.2, 1.0, "File too large for direct export, using alternative method: " + fileName);
                    String downloadLink = fetchExportLinksFromFileId(fileID, mime);
                    downloadFromExportLinkInto(credential.getAccessToken(), downloadLink, target, pc, fileName);
                }
            } else {
                pc.updateProgress(0.1, 1.0, "Downloading binary file: " + fileName);
                try {
                    // Try direct download
                    driveService.files().get(fileID)
                            .setSupportsAllDrives(true)
                            .executeMediaAndDownloadTo(target);
                    pc.updateProgress(1.0, 1.0, "Downloaded: " + fileName);
                } catch (com.google.api.client.http.HttpResponseException e) {
                    // Handle "schrodinger's file" scenario with binary URL
                    pc.updateProgress(0.2, 1.0, "Access denied, trying alternative download method: " + fileName);
                    String downloadLink = d.getBinaryURL();
                    if (downloadLink != null) {
                        downloadFromExportLinkInto(credential.getAccessToken(), downloadLink, target, pc, fileName);
                    } else {
                        throw new IOException("Unable to download file: " + e.getMessage());
                    }
                }
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("Unable to download file: " + e.getDetails());
            pc.updateProgress(0.0, 1.0, "Failed to download: " + fileName + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Static method for backward compatibility.
     * @deprecated Use instance method with dependency injection instead
     */
    @Deprecated
    public static void downloadFromExportLinkInto(String token,
                                                  String link,
                                                  OutputStream target) throws IOException, InterruptedException {
        // Fallback implementation without progress tracking and rate limiting
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
}
