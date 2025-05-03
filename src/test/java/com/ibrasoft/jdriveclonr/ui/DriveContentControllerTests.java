package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.auth.GoogleOAuthService;
import com.ibrasoft.jdriveclonr.service.DriveAPIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class DriveContentControllerTests {

    private DriveAPIService driveAPIService;

    @BeforeEach
    void setUp() throws GeneralSecurityException, IOException {
        driveAPIService = new DriveAPIService(GoogleOAuthService.authorize());
    }

}
