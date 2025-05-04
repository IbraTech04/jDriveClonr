package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.DriveItemCell;
import com.ibrasoft.jdriveclonr.service.DriveAPIService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class DriveContentController implements Initializable {

    /* ----------  FXML  ---------- */
    @FXML private TreeView<DriveItem> driveTreeView;
    @FXML private Button           startCloneButton;
    @FXML private VBox             loadingOverlay;
    @Getter private static DriveItem selectedRoot;

    /* ----------  DI / services  ---------- */
    private DriveAPIService driveService;

    /* ----------  JavaFX lifecycle  ---------- */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Basic TreeView configuration (cells, hide root)
        driveTreeView.setCellFactory(tv -> new DriveItemCell());
        driveTreeView.setShowRoot(true);

        if (loadingOverlay != null) loadingOverlay.setVisible(false);
        if (startCloneButton != null) startCloneButton.setDisable(true);
    }

    /**
     * Injected by the parent controller / FXML loader.
     */
    public void setDriveService(DriveAPIService driveService) {
        this.driveService = driveService;
        loadDriveContent();                  // kick off async fetch now that we have the service
    }

    /* ----------  Fetch & build tree ---------- */
    private void loadDriveContent() {
        if (loadingOverlay != null) loadingOverlay.setVisible(true);

        Task<TreeItem<DriveItem>> loadTask = new Task<>() {
            @Override
            protected TreeItem<DriveItem> call() throws Exception {
                // 1. Fetch data (off FX thread)
                DriveItem ownedRoot   = driveService.fetchOwnedFiles();
                DriveItem sharedRoot  = driveService.fetchSharedFiles();
                DriveItem trashRoot = driveService.fetchTrashedFiles();

                // 2. Build CheckBoxTreeItems (still off FX thread – ok)
                CheckBoxTreeItem<DriveItem> ownedNode  = toCheckBoxTreeItem(ownedRoot);
                CheckBoxTreeItem<DriveItem> sharedNode = toCheckBoxTreeItem(sharedRoot);
                CheckBoxTreeItem<DriveItem> trashNode = toCheckBoxTreeItem(trashRoot);

                // Synthetic invisible root that holds both branches
                DriveItem virtualRootValue = new DriveItem(
                        "virtual-root", "Google Drive", "virtual/root", ownedRoot.getSize() + sharedRoot.getSize(), null, false, new ArrayList<>());
                CheckBoxTreeItem<DriveItem> virtualRoot = new CheckBoxTreeItem<>(virtualRootValue);
                virtualRoot.getChildren().addAll(ownedNode, sharedNode, trashNode);
                virtualRoot.setSelected(true);
                return virtualRoot;
            }
        };

        loadTask.setOnSucceeded(ev -> {
            TreeItem<DriveItem> root = loadTask.getValue();
            driveTreeView.setRoot(root);

            // Enable / disable clone button when selections change
            root.addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(),
                    e -> updateCloneButtonState());

            updateCloneButtonState();            // initial state
            if (loadingOverlay != null) loadingOverlay.setVisible(false);
        });

        loadTask.setOnFailed(ev -> {
            showError("Failed to load Drive content: "
                    + Optional.ofNullable(loadTask.getException())
                    .map(Throwable::getMessage).orElse("unknown error"));
            if (loadingOverlay != null) loadingOverlay.setVisible(false);
        });

        Thread t = new Thread(loadTask, "DriveContentLoader");
        t.setDaemon(true);
        t.start();
    }

    /* ----------  Helpers ---------- */

    /** Recursively converts a DriveItem hierarchy into CheckBoxTreeItems. */
    private CheckBoxTreeItem<DriveItem> toCheckBoxTreeItem(DriveItem node) {
        CheckBoxTreeItem<DriveItem> treeItem = new CheckBoxTreeItem<>(node, null, true);
        for (DriveItem child : node.getChildren()) {
            treeItem.getChildren().add(toCheckBoxTreeItem(child));
        }
        return treeItem;
    }

    private void updateCloneButtonState() {
        if (startCloneButton == null) return;
        boolean any = anySelected((CheckBoxTreeItem<DriveItem>) driveTreeView.getRoot());
        startCloneButton.setDisable(!any);
    }

    private boolean anySelected(CheckBoxTreeItem<DriveItem> item) {
        if (item == null) return false;

        if (item.isSelected() && !item.getValue().getMimeType().startsWith("virtual/")) {
            return true;
        }
        for (TreeItem<DriveItem> child : item.getChildren()) {
            if (anySelected((CheckBoxTreeItem<DriveItem>) child)) return true;
        }
        return false;
    }

    /**
     /**
     * Returns a DriveItem tree containing only selected items with their hierarchy preserved.
     * @param item The root checkbox tree item
     * @return A new DriveItem tree with only selected nodes
     */
    public static DriveItem collectSelected(CheckBoxTreeItem<DriveItem> item, boolean print) {
        if (item == null || (!item.isSelected() && !item.isIndeterminate())) return null;

        DriveItem original = item.getValue();
        DriveItem copy = new DriveItem(
                original.getId(),
                original.getName(),
                original.getMimeType(),
                original.getSize(),
                original.getModifiedTime(),
                original.isShared(),
                new ArrayList<>()
        );

        for (TreeItem<DriveItem> child : item.getChildren()) {
            DriveItem selectedChild = collectSelected((CheckBoxTreeItem<DriveItem>) child, false);
            if (selectedChild != null) {
                copy.getChildren().add(selectedChild);
            }
        }
        if (print) {
            System.out.print(copy);
        }
        return copy;
    }

    /* ----------  UI actions ---------- */

    @FXML private void onBackClicked() { 
        App.navigateTo("auth.fxml");
    }

    @FXML
    private void onStartCloneClicked() {
        DriveItem selected = collectSelected((CheckBoxTreeItem<DriveItem>) driveTreeView.getRoot(), true);
        if (selected == null) {
            showAlert("No items selected", "Please select items to clone before starting.");
            return;
        }
        selectedRoot = selected;

        try {
            App.navigateTo("config.fxml");
        } catch (Exception e) {
            showError("Could not load download view: " + e.getMessage());
        }
    }

    /* ----------  Dialog helpers ---------- */

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error"); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
        });
    }
}
