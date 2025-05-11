package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DriveAPIService {
    private final Drive driveService;
    private final Credential googleCreds;


    public DriveAPIService(Credential credential) throws GeneralSecurityException, IOException {
        this.googleCreds = credential;
        this.driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("DriveClonr")
                .build();
    }

    /**
     * Private wrapper around {@link Drive#files().list()} to fetch files with a specified query.
     *
     * @param query A Google Drive API V3 query string.
     * @return A list of files matching the query.
     * @throws IOException If the request fails or an I/O error occurs.
     */
    private List<File> fetchFiles(String query) throws IOException {
        List<File> files = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setQ(query + " and mimeType != 'application/vnd.google-apps.form'"
                            + "and mimeType != 'application/vnd.google-apps.shortcut' and mimeType != 'application/vnd.google-apps.drive-sdk'"
                    )
                    .setFields("nextPageToken, files(id, name, mimeType, parents, modifiedTime, size, shared, webContentLink)")
                    .setPageToken(pageToken)
                    .setPageSize(1000)
                    .setSupportsAllDrives(true)
                    .setCorpora("allDrives")
                    .setIncludeItemsFromAllDrives(true)
                    .execute();
            files.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return files;
    }

    /**
     * Returns a DriveItem tree representing the users root-owned items.
     * This is a virtual root node that contains all the files, and which implements lazy loading for all subtrees for
     * memory and performance benefits
     *
     * @return A virtual root representing all the files in the user's Drive
     * @throws IOException If the request fails or an I/O error occurs.
     */
    public DriveItem fetchRootOwnedItems() throws IOException {
        DriveItem virtualRoot =
                new DriveItem("root", "My Files", "virtual/root",
                        0, null, false, new ArrayList<>(), () -> {
                    try {
                        return convertFileToDriveItems(fetchFiles("'root' in parents and trashed = false and 'me' in owners"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, null);

        List<File> files = fetchFiles("'root' in parents and trashed = false and 'me' in owners");
        virtualRoot.setChildren(convertFileToDriveItems(files));

        return virtualRoot;
    }

    /**
     * Returns a DriveItem tree representing the users root-shared items.
     * This is a virtual root node that contains all the files, and which implements lazy loading for all subtrees for
     * memory and performance benefits
     *
     * @return A virtual root representing all the files which are shared with the user
     * @throws IOException If the request fails or an I/O error occurs.
     */
    public DriveItem fetchRootSharedItems() throws IOException {
        DriveItem virtualRoot =
                new DriveItem("shared-root", "Shared With Me", "virtual/root",
                        0, null, false, new ArrayList<>(), () -> {
                    try {
                        List<File> files = fetchFiles("(trashed = false) and sharedWithMe");
                        return (convertFileToDriveItems(files.stream()
                                .filter(file -> file.getParents() == null || file.getParents().isEmpty())
                                .toList()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, null);

        List<File> files = fetchFiles("(trashed = false) and sharedWithMe");
        virtualRoot.setChildren(convertFileToDriveItems(files.stream()
                .filter(file -> file.getParents() == null || file.getParents().isEmpty())
                .toList()));
        return virtualRoot;
    }

    /**
     * Beta Feature: Shared Drives Cloning
     * No Javadoc here because this is highly experimental and not yet ready for production
     *
     * @param driveId The ID of the shared drive to fetch files from
     * @return A list of files in the shared drive
     * @throws IOException If the request fails or an I/O error occurs.
     */
    private List<com.google.api.services.drive.model.File> fetchFilesInDrive(String driveId) throws IOException {
        FileList fileList = driveService.files().list()
                .setQ(String.format("'%s' in parents and trashed = false", driveId))
                .setDriveId(driveId)
                .setCorpora("drive")
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id, name, mimeType, parents, size)")
                .execute();

        return fileList.getFiles();
    }


    public DriveItem fetchRootSharedDrives() throws IOException {
        Drive.Drives.List request = driveService.drives().list()
                .setPageSize(100)
                .setFields("drives(id, name, createdTime)");

        List<com.google.api.services.drive.model.Drive> drives = request.execute().getDrives();

        DriveItem virtualRoot = new DriveItem(
                "virtual-shared-root", "Shared Drives", "virtual/root",
                0, null, false, new ArrayList<>(), null, null);

        for (com.google.api.services.drive.model.Drive drive : drives) {
            DriveItem sharedDriveItem = new DriveItem(
                    drive.getId(),
                    drive.getName(),
                    "virtual/root",
                    0,
                    null,
                    true,
                    List.of(),
                    () -> {
                        try {
                            return convertFileToDriveItems(
                                    fetchFilesInDrive(drive.getId())
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, null);

            virtualRoot.getChildren().add(sharedDriveItem);
        }

        return virtualRoot;
    }

    /**
     * Similar to the above methods; this method fetches the files in the trash
     * and creates a virtual root node for them
     * Again, all lazily-loaded :P
     *
     * @return A virtual root representing all the files in the trash
     * @throws IOException If the request fails or an I/O error occurs.
     */
    public DriveItem fetchRootTrashedItems() throws IOException {
        DriveItem virtualRoot =
                new DriveItem("root", "Trash", "virtual/root",
                        0, null, false, new ArrayList<>(), () -> {
                    try {
                        return convertFileToDriveItems(fetchFiles("'root' in parents and trashed = true"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, null);

        List<File> files = fetchFiles("'root' in parents and trashed = true");
        virtualRoot.setChildren(convertFileToDriveItems(files));

        return virtualRoot;
    }

    /**
     * Converts a list of {@link File} objects to a list of {@link DriveItem} objects.
     * Also sets the children of each DriveItem to a lazy-loaded list of DriveItems.
     *
     * @param files The list of files to convert
     * @return A list of DriveItem objects
     */
    public List<DriveItem> convertFileToDriveItems(List<File> files) {
        List<DriveItem> driveItems = new ArrayList<>();
        for (File file : files) {
            DriveItem driveItem = new DriveItem(
                    file.getId(),
                    file.getName(),
                    file.getMimeType(),
                    file.getSize() == null ? 0 : file.getSize(),
                    file.getModifiedTime(),
                    file.getShared() != null? file.getShared() : false,
                    new ArrayList<>(),
                    () -> {
                        try {
                            return convertFileToDriveItems(fetchFiles("'" + file.getId() + "' in parents"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    file.getWebContentLink() == null ? null : file.getWebContentLink()

            );
            driveItems.add(driveItem);
        }
        return driveItems;
    }

    @Deprecated
    public ByteArrayOutputStream downloadFile(String fileID, ExportFormat mime) throws IOException {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // If the exportMIME is not null, we need to export the file. Otherwise, request the bytes directly
            if (mime != ExportFormat.DEFAULT) {
                // Export the file
                try {
                    this.driveService.files().export(fileID, mime.getMimeType())
                            .executeMediaAndDownloadTo(outputStream);
                } catch (IOException e) {
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
     * Unlike {@link #downloadFile(String, ExportFormat)}, this method does not load the file into memory
     * This method allows for pushing files into an arbitrary instance of {@link OutputStream}, allowing
     * for streaming the file to a different location.
     *
     * @param d The DriveItem to download
     * @param mime   The export format to use
     * @param target The target output stream to write the file to
     */
    public void downloadInto(DriveItem d, ExportFormat mime, OutputStream target) throws IOException, InterruptedException {
        String fileID = d.getId();

        try {
            if (mime != ExportFormat.DEFAULT) {
                try {
                    this.driveService.files().export(fileID, mime.getMimeType())
                            .executeMediaAndDownloadTo(target);
                } catch (IOException e) {
                    // We have likely run into a scenario where the file is too big to be exported, therefore we must
                    // Use the export links trick.
                    String downloadLink = fetchExportLinksFromFileId(fileID, mime);
                    downloadFromExportLinkInto(this.googleCreds.getAccessToken(), downloadLink, target);
                }
            } else {
                try {
                    this.driveService.files().get(fileID)
                            .setSupportsAllDrives(true)
                            .executeMediaAndDownloadTo(target);
                } catch (com.google.api.client.http.HttpResponseException e) {
                    // We have likely encountered a "schrodinger's file" scenario where the file is somehow both shared
                    // and not shared with us => Use Binary Export Links trick

                    String downloadLink = d.getBinaryURL();
                    if (downloadLink != null) {
                        downloadFromExportLinkInto(this.googleCreds.getAccessToken(), downloadLink, target);
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

    /**
     * Returns the export link for a given file ID and export format.
     *
     * @param fileId the ID of the file to export
     * @param mime   the export format to use
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
    @Deprecated
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

    public static void downloadFromExportLinkInto(String token,
                                                  String link,
                                                  OutputStream target) throws IOException, InterruptedException {
        HttpResponse<InputStream> response;
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder(URI.create(link))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        }

        if (response.statusCode() != 200) {
            String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("Failed to download: HTTP " + response.statusCode() + " – " + err);
        }

        // Still just a stream‑to‑stream pipe
        try (InputStream in = response.body()) {
            in.transferTo(target);
        }
    }


    /**
     * Used to create a copy of the current running instance for Thread Safety, because the Google Drive API is *not*
     * Thread-safe by default
     *
     * @return A new instance of DriveAPIService
     * @throws GeneralSecurityException If the credentials are invalid or cannot be loaded
     * @throws IOException If anything goes wrong with loading the credentials
     */
    public DriveAPIService createCopy() throws GeneralSecurityException, IOException {
        return new DriveAPIService(this.googleCreds);
    }

}
