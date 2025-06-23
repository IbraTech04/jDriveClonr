package com.ibrasoft.jdriveclonr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.slides.v1.Slides;
import com.google.common.util.concurrent.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class GoogleServiceFactory {
    
    private static final RateLimiter rateLimiter = RateLimiter.create(1);
    
    public static GoogleServices createServices(Credential credential) throws Exception {
        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();
        
        Drive drive = new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("DriveClonr")
                .build();
                
        Sheets sheets = new Sheets.Builder(transport, jsonFactory, credential)
                .setApplicationName("DriveClonr")
                .build();
                
        Slides slides = new Slides.Builder(transport, jsonFactory, credential)
                .setApplicationName("DriveClonr")
                .build();
                
        return new GoogleServices(drive, sheets, slides, credential, rateLimiter);
    }
    
    @AllArgsConstructor
    @Getter
    public static class GoogleServices {
        private final Drive driveService;
        private final Sheets sheetsService;
        private final Slides slidesService;
        private final Credential credential;
        private final RateLimiter rateLimiter;
    }
}
