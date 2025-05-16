package com.ibrasoft.jdriveclonr.export;

import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.model.GoogleMime;
import com.ibrasoft.jdriveclonr.service.ServiceRepository;
import com.ibrasoft.jdriveclonr.utils.FileUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Data
@NoArgsConstructor
public class GoogleSheetsExporter implements IDocumentExporter {

    final GoogleMime SUPPORTED_MIME = GoogleMime.SHEETS;

    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format) throws IOException, InterruptedException {
        if (!format.isPrimitive()) {
            ExportUtils.downloadNormally(d, filePath, format);
        }
        else{
            // If it *is* primitive => We need to
            // A) Create a folder with the current driveitem name
            // B) Iterate over all sub-documents and export them one-by-one into the folder

            File dest = new File (filePath, d.getName());

            if (!dest.mkdir()) {
                throw new IOException("Failed to create directory: " + filePath + d.getName());
            }

            Spreadsheet sheet = ServiceRepository.getSheetsService().spreadsheets().get(d.getId()).execute();
            for (Sheet s: sheet.getSheets()) {
                String sheetName = s.getProperties().getTitle();
                sheetName = FileUtils.sanitizeFilename(sheetName);
                Integer gid = s.getProperties().getSheetId();

                String exportUrl = String.format(
                        "https://docs.google.com/spreadsheets/d/%s/gviz/tq?tqx=out:%s&gid=%s",
                        d.getId(), format.getShortMime(), gid
                );
                File outFile = new File(dest, sheetName + format.getExtension());

                try (FileOutputStream output = new FileOutputStream(outFile)) {
                    ExportUtils.downloadFromExportLinkInto(
                            ServiceRepository.getCredential().getAccessToken(),
                            exportUrl,
                            output
                    );
                }

            }
        }
    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return d.getMimeType().equals(this.SUPPORTED_MIME.getMimeType());
    }
}
