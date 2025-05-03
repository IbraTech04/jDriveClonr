package com.ibrasoft.jdriveclonr.service;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Thin wrapper: splits files evenly into N worker tasks,
 * aggregates overall progress, supports cancel/shutdown.
 */
public class DownloadService {

    private ExecutorService pool;
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);
    private final BooleanProperty running        = new SimpleBooleanProperty(false);
    private final StringProperty  status         = new SimpleStringProperty("");

    public ReadOnlyDoubleProperty overallProgressProperty() { return overallProgress; }
    public ReadOnlyBooleanProperty runningProperty()        { return running; }
    public ReadOnlyStringProperty  statusProperty()         { return status; }
    public boolean isFinished()                              { return !running.get(); }

    public void start(List<DriveItem> files, int threads, Consumer<Task<?>> uiConsumer) {
        if (running.get()) return;
        running.set(true);

        pool = Executors.newFixedThreadPool(threads);

        int slice = (int) Math.ceil(files.size() / (double) threads);
        int idx   = 0;
        for (int t = 0; t < threads && idx < files.size(); t++) {
            List<DriveItem> bucket = files.subList(idx, Math.min(idx + slice, files.size()));
            idx += slice;
            Task<Void> task = createTask(bucket, "Thread‑" + (t + 1));
            pool.submit(task);
            Platform.runLater(() -> uiConsumer.accept(task));
        }

        // Watcher: shutdown when all done
        CompletableFuture.runAsync(() -> {
            try { pool.shutdown(); pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> running.set(false));
        });
    }

    public void cancelAll() { if (pool != null) pool.shutdownNow(); }
    public void shutdownNow() { cancelAll(); }

    /* -------- internal Task generator -------- */
    private Task<Void> createTask(List<DriveItem> bucket, String title) {
        return new Task<>() {
            @Override protected Void call() throws Exception {
                updateTitle(title);
                long done = 0, total = bucket.stream().mapToLong(DriveItem::getSize).sum();

                for (DriveItem d : bucket) {
                    if (isCancelled()) break;
                    updateMessage("Downloading " + d.getName());
                    downloadFile(d);
                    done += Math.max(1, d.getSize());
                    updateProgress(done, total);
                    updateOverall(done);
                }
                return null;
            }
        };
    }

    /* -------- simplistic Google‑Drive download -------- */
    private void downloadFile(DriveItem d) throws IOException {
        // TODO  ►  Use Drive API’s Files.get(d.getId()).executeMediaAndDownloadTo(outStream)
        // Below is just a dummy sleep to simulate work
        try { Thread.sleep(300 + (long)(Math.random() * 700)); } catch (InterruptedException ignored) {}
    }

    /* -------- aggregate overall -------- */
    private final Object lock = new Object();
    private long globalDone = 0, globalTotal = 1;
    private void updateOverall(long delta) {
        synchronized (lock) {
            globalDone += delta;
            overallProgress.set(globalDone / (double) globalTotal);
        }
    }
}
