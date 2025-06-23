package com.ibrasoft.jdriveclonr.model;

import com.google.api.client.util.DateTime;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor
@Data
public class DriveItem {
    private static final Logger logger = LoggerFactory.getLogger(DriveItem.class);

    private String id;
    private String name;
    private String mimeType;
    private long size;
    private DateTime modifiedTime;
    private boolean shared;
    private List<DriveItem> children;
    private Supplier<List<DriveItem>> next;
    private String binaryURL = null;

    public DriveItem() {
        logger.info("DriveItem instance created");
    }

    public void setChildren(List<DriveItem> children) {
        // sort children based on type and name
        children = children.stream()
                .sorted(Comparator.comparing(DriveItem::isFolder).reversed()
                        .thenComparing(DriveItem::getName))
                .collect(Collectors.toList());
        this.children = children;
    }

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

    public void loadChildren() {
        if (!this.isLoaded()){
            List<DriveItem> loadedChildren = next != null ? next.get() : List.of();
            this.setChildren(loadedChildren);
        }
    }

    private String toStringHelper(int indentAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ".repeat(Math.max(0, indentAmount)));
        sb.append(name).append("\n");
        if (children != null) {
            for (DriveItem child : children) {
                sb.append(child.toStringHelper(indentAmount + 1));
            }
        }
        return sb.toString();
    }

    public CheckBoxTreeItem<DriveItem> toLazyTreeItem() {
        CheckBoxTreeItem<DriveItem> treeItem = new CheckBoxTreeItem<>(this);
        treeItem.setExpanded(false);

        if (this.isFolder()) {
            // Add a real "Loading..." item to show while fetching
            DriveItem loadingItem = new DriveItem("loading", "Loading...", "", 0, null, false, List.of(), null, null);
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
        return children.size() == 1 && "loading".equals(children.getFirst().getValue().getId());
    }

    private void loadChildrenAsync(CheckBoxTreeItem<DriveItem> parent, DriveItem item) {
        Task<List<DriveItem>> loadTask = new Task<>() {
            @Override
            protected List<DriveItem> call()  {
                System.out.println("Loading children for: " + item.getId());
                if (!item.getChildren().isEmpty())
                    return item.getChildren();
                if (item.getNext() != null)
                    return item.getNext().get();
                return List.of(new DriveItem("empty", "No items", "", 0, null, false, List.of(), null, null));
            }
        };

        loadTask.setOnSucceeded(evt -> {
            List<DriveItem> children = loadTask.getValue();

            parent.getChildren().clear();

            if (children.isEmpty()) {
                DriveItem emptyItem = new DriveItem("empty", "No items", "", 0, null, false, List.of(), null, null);
                parent.getChildren().add(new CheckBoxTreeItem<>(emptyItem));
                return;
            }
            // sort children based on type and name
            children = children.stream()
                    .sorted(Comparator.comparing(DriveItem::isFolder).reversed()
                            .thenComparing(DriveItem::getName))
                    .toList();
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
            DriveItem errorItem = new DriveItem("error", "Failed to load", "", 0, null, false, List.of(), null, null);
            parent.getChildren().add(new CheckBoxTreeItem<>(errorItem));
        });

        new Thread(loadTask).start();
    }

    /**
     * Checks if the DriveItem's children have been fully loaded, or if they are still lazy-loaded.
     * @return true if the children are loaded, false otherwise.
     */
    public boolean isLoaded(){
        return children != null && !children.isEmpty() && !children.getFirst().getId().equals("loading");
    }

}
