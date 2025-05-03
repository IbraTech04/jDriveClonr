package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

public class DriveAPIService {
    private final Drive driveService;

    public DriveAPIService(Credential credential) throws GeneralSecurityException, IOException {
        this.driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("DriveClonr")
                .build();
    }

    public DriveItem fetchOwnedFiles() throws IOException {
        return convertFilesToDriveItemTree(fetchFiles("'me' in owners and trashed = false and mimeType != 'application/vnd.google-apps.form'"), "My Files");
    }

    public DriveItem fetchSharedFiles() throws IOException {
        return convertFilesToDriveItemTree(fetchFiles("sharedWithMe = true and trashed = false and mimeType != 'application/vnd.google-apps.form'"), "Shared with Me");
    }

    public DriveItem fetchTrashedFiles() throws IOException {
        return convertFilesToDriveItemTree(fetchFiles("trashed = true"), "Trash and mimeType != 'application/vnd.google-apps.form'");
    }

    private List<File> fetchFiles(String query) throws IOException {
        List<File> files = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setQ(query)
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

        /* ---------- Optional: detect *truly* lost nodes (never attached) ------- */
        // (Not required for normal runs; useful for diagnostics.)
    /*
    for (String id : idToItem.keySet()) {
        if (!id.equals("virtual-root") && !attachedAsChild.contains(id)
            && virtualRoot.getChildren().stream().noneMatch(di -> di.getId().equals(id))) {
            System.err.println("Warning: unattached file " + idToItem.get(id).getName());
            virtualRoot.getChildren().add(idToItem.get(id));
        }
    }
    */

        return virtualRoot;
    }

}
