package com.ibrasoft.jdriveclonr.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum ExportFormat {
    DOCX("Microsoft Word (.docx)", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    XLSX("Microsoft Excel (.xlsx)", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    PPTX("Microsoft PowerPoint (.pptx)", "application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
    PDF("PDF (.pdf)", "application/pdf", ".pdf"),
    PNG("PNG Image (.png)", "image/png", ".png"),
    JPEG("JPEG Image (.jpg)", "image/jpeg", ".jpg"),
    SVG("SVG Vector (.svg)", "image/svg+xml", ".svg"),
    TXT("Plain Text (.txt)", "text/plain", ".txt"),
    HTML("HTML (.html)", "text/html", ".html"),
    CSV("CSV (.csv)", "text/csv", ".csv"),

    MARKDOWN("Markdown (.md)", "text/markdown", ".md");

    private final String uiLabel;
    private final String mimeType;
    private final String extension;

    ExportFormat(String uiLabel, String mimeType, String extension) {
        this.uiLabel = uiLabel;
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public static ExportFormat fromUiLabel(String label) {
        for (ExportFormat format : values()) {
            if (format.uiLabel.equalsIgnoreCase(label)) return format;
        }
        return null;
    }

    public static List<ExportFormat> getFormatsForGoogleMime(String googleMimeType) {
        List<ExportFormat> list = new ArrayList<>();
        switch (googleMimeType) {
            case GoogleMime.DOCS -> list.addAll(List.of(DOCX, PDF, MARKDOWN, TXT, HTML));
            case GoogleMime.SHEETS -> list.addAll(List.of(XLSX, PDF, CSV, HTML));
            case GoogleMime.SLIDES -> list.addAll(List.of(PPTX, PDF, TXT, HTML));
            case GoogleMime.DRAWINGS -> list.addAll(List.of(PNG, JPEG, SVG, PDF));
            case GoogleMime.JAMBOARD -> list.addAll(List.of(PDF, PNG));
        }
        return list;
    }
}
