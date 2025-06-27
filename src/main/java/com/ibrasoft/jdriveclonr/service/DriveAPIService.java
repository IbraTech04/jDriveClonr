package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.mime.ExportFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
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

    public DriveAPIService(Credential credential) throws GeneralSecurityException, IOException {
        this.driveService = ServiceRepository.getDriveService();
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
                0, null, false, new ArrayList<>(),
                () -> convertDrivesToDriveItems(drives), null);

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
                    },
                    null);

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

    public List<DriveItem> convertDrivesToDriveItems(List<com.google.api.services.drive.model.Drive> drives) {
        List<DriveItem> driveItems = new ArrayList<>();
        for (com.google.api.services.drive.model.Drive drive : drives) {
            DriveItem driveItem = new DriveItem(
                    drive.getId(),
                    drive.getName(),
                    "virtual/root",
                    0,
                    null,
                    true,
                    new ArrayList<>(),
                    () -> {
                        try {
                            return convertFileToDriveItems(fetchFilesInDrive(drive.getId()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    null
            );
            driveItems.add(driveItem);
        }
        return driveItems;
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
}
