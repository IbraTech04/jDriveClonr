package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.common.util.concurrent.RateLimiter;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.mime.ExportFormat;
import com.ibrasoft.jdriveclonr.model.mime.GoogleMime;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Data
@AllArgsConstructor
public class GoogleSheetsExporter implements IDocumentExporter {

    private final Sheets sheetsService;
    private final Credential credential;
    final GoogleMime SUPPORTED_MIME = GoogleMime.SHEETS;
    private RateLimiter rateLimiter;

    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format, ProgressCallback pc) throws IOException, InterruptedException {
        // If it *is* primitive => We need to
        // A) Create a folder with the current driveitem name
        // B) Iterate over all sub-documents and export them one-by-one into the folder

        // add file id at the end to avoid conflicts with other files
        // recall that google drive allows duplicate file names for google workspace documents
        File dest = new File(filePath, FileUtils.sanitizeFilename(d.getName() + " - " + d.getId().substring(0, 8)));

        if (!dest.mkdir()) {
            throw new IOException("Failed to create directory: " + filePath + d.getName());
        }

        Spreadsheet sheet = sheetsService.spreadsheets().get(d.getId())
                .setFields("sheets.properties.title,sheets.properties.sheetId")
                .execute();

        for (int i = 0; i < sheet.getSheets().size(); i++) {
            rateLimiter.acquire();
            Sheet s = sheet.getSheets().get(i);
            String sheetName = s.getProperties().getTitle();
            sheetName = FileUtils.sanitizeFilename(sheetName);
            Integer gid = s.getProperties().getSheetId();
            pc.updateProgress((i / (1.0 * sheet.getSheets().size())), 1.0, "Exporting sheet: " + sheetName);

            String exportUrl = String.format(
                    "https://docs.google.com/spreadsheets/d/%s/gviz/tq?tqx=out:%s&gid=%s",
                    d.getId(), format.getShortMime(), gid
            );
            File outFile = new File(dest, sheetName + format.getExtension());
            try (FileOutputStream output = new FileOutputStream(outFile)) {
                DefaultExporter.downloadFromExportLinkInto(
                        credential.getAccessToken(),
                        exportUrl,
                        output
                );
            }
            pc.updateProgress((i + 1 / (1.0 * sheet.getSheets().size())), 1.0, "Exporting sheet: " + sheetName);
        }
    }


    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals(this.SUPPORTED_MIME.getMimeType());
    }
}
