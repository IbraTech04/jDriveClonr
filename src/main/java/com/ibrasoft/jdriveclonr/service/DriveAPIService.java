package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;

public class DriveAPIService {
    private final Drive driveService;
    private final Credential googleCreds;


    public DriveAPIService(Credential credential) throws GeneralSecurityException, IOException {
        this.googleCreds = credential;
        this.driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("DriveClonr")
                .build();
    }

    private List<File> fetchFiles(String query) throws IOException {
        List<File> files = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setQ(query + " and mimeType != 'application/vnd.google-apps.form'" +
                            "and mimeType != 'application/vnd.google-apps.shortcut' and mimeType != 'application/vnd.google-apps.drive-sdk'")
                    .setFields("nextPageToken, files(id, name, mimeType, parents, modifiedTime, size, shared)")
                    .setPageToken(pageToken)
                    .setPageSize(1000)
                    .execute();
            files.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return files;
    }

    public DriveItem fetchRootOwnedItems() throws IOException {
        DriveItem virtualRoot =
                new DriveItem("root", "My Files", "virtual/root",
                        0, null, false, new ArrayList<>(), () -> {
                            try {
                                return convertFileToDriveItems(fetchFiles("'root' in parents and trashed = false and 'me' in owners"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

        List<File> files = fetchFiles("'root' in parents and trashed = false and 'me' in owners");
        virtualRoot.setChildren(convertFileToDriveItems(files));

        return virtualRoot;
    }

    public DriveItem fetchRootSharedItems() throws IOException {
        DriveItem virtualRoot =
                new DriveItem("root", "Shared With Me", "virtual/root",
                        0, null, false, new ArrayList<>(), () -> {
                            try {
                                return convertFileToDriveItems(fetchFiles("(trashed = false) and sharedWithMe"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

        List<File> files = fetchFiles("(trashed = false) and sharedWithMe");
        virtualRoot.setChildren(convertFileToDriveItems(files.stream()
                .filter(file -> file.getParents() == null || file.getParents().isEmpty())
                .toList()));

        return virtualRoot;
    }

    public DriveItem fetchRootTrashedItems() throws IOException {
        DriveItem virtualRoot =
                new DriveItem("root", "Trash", "virtual/root",
                        0, null, false, new ArrayList<>(), () -> {
                            try {
                                return convertFileToDriveItems(fetchFiles("'root' in parents and trashed = true"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

        List<File> files = fetchFiles("'root' in parents and trashed = true");
        virtualRoot.setChildren(convertFileToDriveItems(files));

        return virtualRoot;
    }

    public List<DriveItem> convertFileToDriveItems(List<File> files) {
        List<DriveItem> driveItems = new ArrayList<>();
        for (File file : files) {
            DriveItem driveItem = new DriveItem(
                    file.getId(),
                    file.getName(),
                    file.getMimeType(),
                    file.getSize() == null ? 0 : file.getSize(),
                    file.getModifiedTime(),
                    file.getShared(),
                    new ArrayList<>(),
                    () -> {
                        try {
                            return convertFileToDriveItems(fetchFiles("'" + file.getId() + "' in parents"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            driveItems.add(driveItem);
        }
        return driveItems;
    }

    public ByteArrayOutputStream downloadFile(String fileID, ExportFormat mime) throws IOException {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // If the exportMIME is not null, we need to export the file. Otherwise, request the bytes directly
            if (mime != ExportFormat.DEFAULT) {
                // Export the file
                try {
                    this.driveService.files().export(fileID, mime.getMimeType())
                            .executeMediaAndDownloadTo(outputStream);
                }
                catch (IOException e){
                    // We have likely run into a scenario where the file is too big to be exported, therefore we must
                    // Use the export links trick.
                    String downloadLink = fetchExportLinksFromFileId(fileID, mime);
                    return downloadFromExportLink(this.googleCreds.getAccessToken(), downloadLink);
                }
            } else {
                // Download the file
                this.driveService.files().get(fileID)
                        .executeMediaAndDownloadTo(outputStream);
            }

            return outputStream;
        } catch (GoogleJsonResponseException e) {
            System.err.println("Unable to move file: " + e.getDetails());
            throw e;
        }
    }

    /**
     * Returns the export link for a given file ID and export format.
     * @param fileId the ID of the file to export
     * @param mime the export format to use
     * @return the export link for the file
     * @throws IOException if the request fails or an I/O error occurs
     */
    public String fetchExportLinksFromFileId(String fileId, ExportFormat mime) throws IOException {
        File file = driveService.files().get(fileId)
                .setFields("exportLinks")
                .execute();
        Map<String, String> exportLinks = file.getExportLinks();
        return exportLinks.get(mime.getMimeType());
    }

    /**
     * Downloads a file from a given export link using the provided token.
     * Streams the response into a {@link java.io.ByteArrayOutputStream} so it
     * works on Java8 (no{@code InputStream.readAllBytes}) and avoids holding
     * two copies of the data in memory.
     *
     * @param token the OAuth2 bearer token
     * @param link  the Google Drive “export” URL
     * @return the downloaded file contents
     * @throws IOException if the request fails or an I/O error occurs
     */
    public static ByteArrayOutputStream downloadFromExportLink(String token, String link) throws IOException {
        URL url = new URL(link);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);

        // Buffer size: 8KiB is a good default
        final int BUFFER_SIZE = 8 * 1024;

        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return out;
            } else {
                throw new IOException("Failed to download file: HTTP " + conn.getResponseCode());
            }
        }
    }


}
