package com.ibrasoft.jdriveclonr.model;

import com.google.api.client.util.DateTime;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBoxTreeItem;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.scene.control.TreeItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DriveItem {
    private String id;
    private String name;
    private String mimeType;
    private long size;
    private DateTime modifiedTime;
    private boolean shared;
    private List<DriveItem> children;
    private Supplier<List<DriveItem>> next;

    public void clearChildren() {
        children.clear();
    }

    public boolean isFolder() {
        return "application/vnd.google-apps.folder".equalsIgnoreCase(mimeType) ||
                "virtual/root".equalsIgnoreCase(mimeType);
    }

    public String toString() {
        return toStringHelper(0);
    }

    private String toStringHelper(int indentAmount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentAmount; i++) {
            sb.append("  ");
        }
        sb.append(name).append("\n");
        if (children != null) {
            for (DriveItem child : children) {
                sb.append(child.toStringHelper(indentAmount + 1));
            }
        }
        return sb.toString();
    }

    public CheckBoxTreeItem<DriveItem> toCheckBoxTreeItem() {
        CheckBoxTreeItem<DriveItem> itemTree = new CheckBoxTreeItem<>(this, null, true);

        // Sort children such that folders are first, and they're sorted by name
        List<DriveItem> sortedChildren = children.stream()
                .sorted(Comparator.comparing(DriveItem::isFolder).reversed()
                        .thenComparing(DriveItem::getName))
                .collect(Collectors.toList());
        // Recurse over children
        for (DriveItem child : sortedChildren) {
            itemTree.getChildren().add(child.toCheckBoxTreeItem());
        }
        return itemTree;
    }
    public CheckBoxTreeItem<DriveItem> toLazyTreeItem() {
        CheckBoxTreeItem<DriveItem> treeItem = new CheckBoxTreeItem<>(this);
        treeItem.setExpanded(false);

        if (this.isFolder()) {
            // Add a real "Loading..." item to show while fetching
            DriveItem loadingItem = new DriveItem("loading", "Loading...", "", 0, null, false, List.of(), null);
            CheckBoxTreeItem<DriveItem> loadingNode = new CheckBoxTreeItem<>(loadingItem);
            treeItem.getChildren().add(loadingNode);

            treeItem.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                if (isNowExpanded && isPlaceholder(treeItem.getChildren())) {
                    loadChildrenAsync(treeItem, this);
                }
            });
        }

        return treeItem;
    }

    private boolean isPlaceholder(ObservableList<TreeItem<DriveItem>> children) {
        return children.size() == 1 && "loading".equals(children.get(0).getValue().getId());
    }

    private void loadChildrenAsync(CheckBoxTreeItem<DriveItem> parent, DriveItem item) {
        Task<List<DriveItem>> loadTask = new Task<>() {
            @Override
            protected List<DriveItem> call() throws InterruptedException {
//                return fetchChildren(item); // implement this to return children
                System.out.println("Loading children for: " + item.getId());
                if (!item.getChildren().isEmpty())
                    return item.getChildren();
                if (item.getNext() != null)
                    return item.getNext().get();
                return List.of(new DriveItem("empty", "No items", "", 0, null, false, List.of(), null));
            }
        };

        loadTask.setOnSucceeded(evt -> {
            List<DriveItem> children = loadTask.getValue();

            parent.getChildren().clear();

            if (children.isEmpty()) {
                DriveItem emptyItem = new DriveItem("empty", "No items", "", 0, null, false, List.of(), null);
                parent.getChildren().add(new CheckBoxTreeItem<>(emptyItem));
                return;
            }

            for (DriveItem child : children) {
                parent.getChildren().add(child.toLazyTreeItem());
            }
            if (parent.isSelected()) {
                for (TreeItem<DriveItem> child : parent.getChildren()) {
                    ((CheckBoxTreeItem<DriveItem>) child).setSelected(true);
                }
            }
        });

        loadTask.setOnFailed(evt -> {
            // Optionally show error
            parent.getChildren().clear();
            DriveItem errorItem = new DriveItem("error", "Failed to load", "", 0, null, false, List.of(), null);
            parent.getChildren().add(new CheckBoxTreeItem<>(errorItem));
        });

        new Thread(loadTask).start();
    }

    /**
     * Checks if the DriveItem's children have been fully loaded, or if they are still lazy-loaded.
     * @return
     */
    public boolean isLoaded(){
        return children != null && !children.isEmpty() && !children.get(0).getId().equals("loading");
    }

}
