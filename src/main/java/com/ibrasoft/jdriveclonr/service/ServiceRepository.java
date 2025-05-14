package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.slides.v1.Slides;
import lombok.Data;

/**
 * Singleton class to manage the creation of Google Drive/Docs/Sheets/etc services
 */
public class ServiceRepository {
    public static Credential credential;
    public static Drive driveService;
    public static Slides slidesService;
    public static Sheets sheetsService;

    public static void init(Credential credential) {
        ServiceRepository.credential = credential;
        try {
            driveService = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("DriveClonr")
                    .build();
            slidesService = new Slides.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("DriveClonr")
                    .build();
            sheetsService = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("DriveClonr")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
