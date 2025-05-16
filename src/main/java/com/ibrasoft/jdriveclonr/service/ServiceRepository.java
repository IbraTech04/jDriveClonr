package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.slides.v1.Slides;
import com.google.api.services.sheets.v4.Sheets;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;

public class ServiceRepository {

    private static final ThreadLocal<Credential> threadLocalCredential = new ThreadLocal<>();
    private static final ThreadLocal<Drive> threadLocalDrive = new ThreadLocal<>();
    private static final ThreadLocal<Slides> threadLocalSlides = new ThreadLocal<>();
    private static final ThreadLocal<Sheets> threadLocalSheets = new ThreadLocal<>();

    @Getter
    private static final RateLimiter rateLimiter = RateLimiter.create(1);

    public static void init(Credential credential) {
        threadLocalCredential.set(credential);
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();

            threadLocalDrive.set(new Drive.Builder(transport, jsonFactory, credential)
                    .setApplicationName("DriveClonr")
                    .build());

            threadLocalSlides.set(new Slides.Builder(transport, jsonFactory, credential)
                    .setApplicationName("DriveClonr")
                    .build());

            threadLocalSheets.set(new Sheets.Builder(transport, jsonFactory, credential)
                    .setApplicationName("DriveClonr")
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Drive getDriveService() {
        return threadLocalDrive.get();
    }

    public static Slides getSlidesService() {
        return threadLocalSlides.get();
    }

    public static Sheets getSheetsService() {
        return threadLocalSheets.get();
    }

    public static Credential getCredential() {
        return threadLocalCredential.get();
    }

    public static void clear() {
        threadLocalCredential.remove();
        threadLocalDrive.remove();
        threadLocalSlides.remove();
        threadLocalSheets.remove();
    }
}
