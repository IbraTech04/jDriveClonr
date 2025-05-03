package com.ibrasoft.jdriveclonr.service;

import com.ibrasoft.jdriveclonr.model.DriveItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

import com.ibrasoft.jdriveclonr.auth.GoogleOAuthService;

public class DriveAPIServiceTests {

    private DriveAPIService driveAPIService;

    @BeforeEach
    void setUp() throws GeneralSecurityException, IOException {
        driveAPIService = new DriveAPIService(GoogleOAuthService.authorize());
    }

    @Test
    void testTreeConstruction() throws IOException {
        DriveItem root = this.driveAPIService.fetchOwnedFiles();
        System.out.println(this.driveAPIService.fetchOwnedFiles());
        assertTrue(true);
    }
}
