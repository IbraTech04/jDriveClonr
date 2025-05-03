package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.service.DownloadService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DownloadController implements javafx.fxml.Initializable {

    /* -------- FXML -------- */
    @FXML private Label          overallLabel;
    @FXML private ProgressBar    overallBar;
    @FXML private ListView<Task<?>> threadList;
    @FXML private Button         cancelBtn;
    @FXML private Button         closeBtn;

    /* -------- DI -------- */
    private final DownloadService service = new DownloadService();  // could be injected

    /* -------- init -------- */
    @Override public void initialize(URL u, ResourceBundle rb) {
        // Custom cell shows progress bar + label for each task
        threadList.setCellFactory(lv -> new ListCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            @Override protected void updateItem(Task<?> task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) { setGraphic(null); setText(null); return; }
                bar.progressProperty().bind(task.progressProperty());
                setText(task.getTitle());
                setGraphic(bar);
            }
        });

        cancelBtn.setOnAction(e -> service.cancelAll());
        closeBtn .setOnAction(e -> ((javafx.stage.Stage) closeBtn.getScene().getWindow()).close());

        closeBtn.disableProperty().bind(
                Bindings.createBooleanBinding(() -> !service.isFinished(), service.runningProperty()));
    }

    /* -------- public API -------- */
    public void startDownloads(List<DriveItem> items, int threadCount) {
        ObservableList<Task<?>> tasks = FXCollections.observableArrayList();
        threadList.setItems(tasks);

        service.start(items, threadCount, task -> {
            // This callback runs on FX thread (see DownloadService)
            tasks.add(task);
            overallBar.progressProperty().bind(service.overallProgressProperty());
            overallLabel.textProperty().bind(service.statusProperty());
        });
    }

    /* -------- shutter -------- */
    public void shutdown() { service.shutdownNow(); }
}