package com.ibrasoft.jdriveclonr.model;

import com.google.api.client.util.DateTime;
import javafx.scene.control.CheckBoxTreeItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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

    public void clearChildren() {
        children.clear();
    }

    public boolean isFolder() {
//        return "application/vnd.google-apps.folder".equalsIgnoreCase(mimeType);
        return !this.children.isEmpty();
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

        // Recurse over children
        for (DriveItem child : this.getChildren()) {
            itemTree.getChildren().add(child.toCheckBoxTreeItem());
        }
        return itemTree;
    }


}
