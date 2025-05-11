package com.ibrasoft.jdriveclonr.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;

public class GoogleOAuthService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthService.class);

    private static final List<String> SCOPES = List.of(
            DriveScopes.DRIVE,
            "https://www.googleapis.com/auth/photoslibrary.readonly"
    );
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Setter
    private static String credentialsFilePath = "credentials.json"; // Default path in current directory

    public static Credential authorize() throws IOException, GeneralSecurityException {
        logger.info("Authorizing user");

        if (!Files.exists(Paths.get(credentialsFilePath))) {
            throw new FileNotFoundException("Credentials file not found: " + credentialsFilePath);
        }

        try (InputStream in = new FileInputStream(credentialsFilePath)) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                    .setAccessType("offline")
                    .build();

            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        }
    }
}