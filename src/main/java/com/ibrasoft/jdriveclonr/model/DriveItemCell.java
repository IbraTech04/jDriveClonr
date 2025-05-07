package com.ibrasoft.jdriveclonr.model;

import com.ibrasoft.jdriveclonr.utils.FileUtils;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;


public class DriveItemCell extends CheckBoxTreeCell<DriveItem> {
    private final HBox container;
    private final Label nameLabel;
    private final Label detailsLabel;

    public DriveItemCell() {
        nameLabel = new Label();
        detailsLabel = new Label();
        detailsLabel.setTextFill(Color.GRAY);
        container = new HBox(5);
        container.getChildren().addAll(nameLabel, detailsLabel);
        HBox.setHgrow(detailsLabel, Priority.ALWAYS);
//        dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a").withZone(ZoneId.systemDefault());
    }


    @Override
    public void updateItem(DriveItem item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        nameLabel.setText(item.getName());
        setText(null);

        StringBuilder details = new StringBuilder();
        if (item.isFolder()) {
            details.append("📁 ");
        } else {
            details.append("📄 ").append(FileUtils.formatSize(item.getSize())).append(" • ");
        }
        if (item.isShared()) details.append("👥 • ");
        detailsLabel.setText(details.toString());

        HBox row = new HBox(6);

        // Only add checkbox if it's not a "loading..." or "empty" item
        if (!"loading".equals(item.getId()) && !"empty".equals(item.getId())) {
            javafx.scene.Node check = getGraphic();
            if (check != null) row.getChildren().add(check);
        }

        row.getChildren().add(container);

        setGraphic(row);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }


}

