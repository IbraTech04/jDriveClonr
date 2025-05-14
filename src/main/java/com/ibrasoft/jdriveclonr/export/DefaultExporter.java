package com.ibrasoft.jdriveclonr.export;

import com.google.api.client.auth.oauth2.Credential;
import com.ibrasoft.jdriveclonr.model.DriveItem;
import com.ibrasoft.jdriveclonr.model.ExportFormat;
import com.ibrasoft.jdriveclonr.service.DriveAPIService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DefaultExporter implements IDocumentExporter {
    @Override
    public void exportDocument(DriveItem d, String filePath, ExportFormat format) {

    }

    @Override
    public boolean supports(DriveItem d, ExportFormat format) {
        return true;
    }
}
