package com.ibrasoft.jdriveclonr.service;

import com.ibrasoft.jdriveclonr.auth.GoogleOAuthService;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DownloadServiceTests {
    private DriveAPIService driveAPIService;

    @BeforeEach
    void setUp() throws GeneralSecurityException, IOException {
        driveAPIService = new DriveAPIService(GoogleOAuthService.authorize());
    }

    @Test
    void testDownloadFileNoExport() throws IOException {
        String fileId = "1FXDpN2j8g41Vbi96yRPgfnvyhXGG7g-8"; // Replace with a valid file ID
        String destinationPath = "C:\\Users\\Cheha\\Desktop\\test.pdf"; // Replace with a valid destination path
        ByteArrayOutputStream output = driveAPIService.downloadFile(fileId, ExportFormat.DEFAULT);
        // Write the output to a file
        try (var out = new FileOutputStream(destinationPath)) {
            output.writeTo(out);
        } catch (IOException e) {
            throw new IOException("Error writing file: " + fileId, e);
        }

        // Comapre content.pdf with the file in the destination path
        Path expectedPath = Paths.get("C:\\Users\\Cheha\\Desktop\\content.pdf");
        Path actualPath = Paths.get(destinationPath);
        assertEquals(-1, Files.mismatch(expectedPath, actualPath));

    }

    @Test
    void testDownloadFileExportToDocx() throws IOException {
        String fileId = "1lF_fypFa4G23OH0TvY3UucpZIvaVsxMc63w2K-zTDTM"; // Replace with a valid file ID
        String destinationPath = "C:\\Users\\Cheha\\Desktop\\test.docx"; // Replace with a valid destination path
        ByteArrayOutputStream output = driveAPIService.downloadFile(fileId, ExportFormat.DOCX);
        // Write the output to a file
        try (var out = new FileOutputStream(destinationPath)) {
            output.writeTo(out);
        } catch (IOException e) {
            throw new IOException("Error writing file: " + fileId, e);
        }

        // Comapre content.docx with the file in the destination path
        Path expectedPath = Paths.get("C:\\Users\\Cheha\\Desktop\\content.docx");
        Path actualPath = Paths.get(destinationPath);
        assertEquals(-1, Files.mismatch(expectedPath, actualPath));

    }

    @Test
    void testDownloadFromExportLinksWorkaround() throws IOException {
        String fileId = "1m_8wXtcS1_LFFYUxlqO2EkrTMHFnmSmm9JSRWdVefsY"; // Replace with a valid file ID
        String destinationPath = "C:\\Users\\Cheha\\Desktop\\test.pptx"; // Replace with a valid destination path
        ByteArrayOutputStream output = driveAPIService.downloadFile(fileId, ExportFormat.PPTX);
        // Write the output to a file
        try (var out = new FileOutputStream(destinationPath)) {
            output.writeTo(out);
        } catch (IOException e) {
            throw new IOException("Error writing file: " + fileId, e);
        }

        // Comapre content.docx with the file in the destination path
        Path expectedPath = Paths.get("C:\\Users\\Cheha\\Desktop\\content.docx");
        Path actualPath = Paths.get(destinationPath);
        assertEquals(-1, Files.mismatch(expectedPath, actualPath));
    }
}
