package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public DriveItem fetchOwnedFiles() throws IOException {
        return convertFilesToDriveItemTree(fetchFiles("'me' in owners and trashed = false and mimeType != 'application/vnd.google-apps.form' and mimeType != 'application/vnd.google-apps.shortcut'"), "My Files");
    }

    public DriveItem fetchSharedFiles() throws IOException {
        return convertFilesToDriveItemTree(fetchFiles("(not 'me' in owners or sharedWithMe = true) and trashed = false and mimeType != 'application/vnd.google-apps.form'"), "Shared with Me");
    }

    public List<File> fetchSharedFilesRaw() throws IOException {
        return fetchFiles("(not 'me' in owners or sharedWithMe = true) and trashed = false and mimeType != 'application/vnd.google-apps.form'");
    }

    public DriveItem fetchTrashedFiles() throws IOException {
        return convertFilesToDriveItemTree(fetchFiles("trashed = true"), "Trash");
    }

    private List<File> fetchFiles(String query) throws IOException {
        List<File> files = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setQ(query + " and mimeType != 'application/vnd.google-apps.form'" +
                            "" + // Not a shortcut
                            "")
                    .setFields("nextPageToken, files(id, name, mimeType, parents, modifiedTime, size, shared)")
                    .setPageToken(pageToken)
                    .setPageSize(1000)
                    .execute();
            files.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return files;
    }

    /**
     * Builds a DriveItem tree from a flat list of Google Drive File objects.
     *
     * @param files flat list returned by the Drive API
     * @param rootName label to show on the synthetic root (e.g. “My Drive”)
     * @return a virtual‑root DriveItem whose children form the full tree
     */
    private DriveItem convertFilesToDriveItemTree(List<File> files, String rootName) {
        DriveItem virtualRoot =
                new DriveItem("virtual-root", rootName, "virtual/root",
                        0, null, false, new ArrayList<>());

        /* ---------------- PASS 1 – build a lookup of every node ---------------- */
        Map<String, DriveItem> idToItem = new HashMap<>();

        for (File f : files) {
            idToItem.put(
                    f.getId(),
                    new DriveItem(
                            f.getId(),
                            f.getName(),
                            f.getMimeType(),
                            f.getSize() == null ? 0 : f.getSize(),
                            f.getModifiedTime(),
                            f.getShared(),
                            new ArrayList<>()
                    ));
        }

        /* ---------------- PASS 2 – wire up parent / child links ---------------- */
        Set<String> attachedAsChild = new HashSet<>();

        for (File f : files) {
            List<String> parents = f.getParents();
            if (parents == null || parents.isEmpty()) {
                virtualRoot.getChildren().add(idToItem.get(f.getId()));
                virtualRoot.setSize(virtualRoot.getSize() + (f.getSize() == null? 0 : f.getSize()));
                continue;
            }

            // Attach to the first parent we actually have in the list.
            // (Google Drive can report multiple parents, but modern UI rarely creates them.)
            boolean linked = false;
            for (String parentId : parents) {
                DriveItem parent = idToItem.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(idToItem.get(f.getId()));
                    parent.setSize(parent.getSize() + (f.getSize() == null? 0 : f.getSize()));
                    attachedAsChild.add(f.getId());
                    linked = true;
                    break;
                }
            }

            // If none of the declared parents were in the download set,
            // treat this as a top‑level item
            if (!linked) {
                virtualRoot.getChildren().add(idToItem.get(f.getId()));
                virtualRoot.setSize(virtualRoot.getSize() + (f.getSize() == null? 0 : f.getSize()));
            }
        }

        return virtualRoot;
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
     * @param fileId
     * @param mime
     * @return
     * @throws IOException
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
