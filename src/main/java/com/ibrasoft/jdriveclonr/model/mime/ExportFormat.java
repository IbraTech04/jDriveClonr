package com.ibrasoft.jdriveclonr.model.mime;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum ExportFormat {
    DOCX("Microsoft Word (.docx)", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    XLSX("Microsoft Excel (.xlsx)", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    PPTX("Microsoft PowerPoint (.pptx)", "application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
    ODT("Open Document Text (.odt)", "application/vnd.oasis.opendocument.text", ".odt"),
    ODS("Open Document Spreadsheet (.ods)", "application/vnd.oasis.opendocument.spreadsheet", ".ods"),
    ODP("Open Document Presentation (.odp)", "application/vnd.oasis.opendocument.presentation", ".odp"),
    PDF("PDF (.pdf)", "application/pdf", ".pdf"),
    PNG("PNG Image (.png)", "image/png", ".png", "png"),
    JPEG("JPEG Image (.jpg)", "image/jpeg", ".jpg", "jpeg"),
    SVG("SVG Vector (.svg)", "image/svg+xml", ".svg", "svg"),
    TXT("Plain Text (.txt)", "text/plain", ".txt"),
    HTML("HTML (.html)", "text/html", ".html"),
    CSV("CSV (.csv)", "text/csv", ".csv", "csv"),
    TSV("TSV (.tsv)", "text/tab-separated-values", ".tsv", "tsv"),
    MARKDOWN("Markdown (.md)", "text/markdown", ".md"),
    ZIPHTML("Zipped HTML Archive (.zip)", "application/zip", ".zip"),
    EPUB("EPUB (.epub)", "application/epub+zip", ".epub"),
    DEFAULT("Default", "application/vnd.google-apps.unknown", "");
    private final String uiLabel;
    private final String mimeType;
    private final String extension;
    private final String shortMime;

    ExportFormat(String uiLabel, String mimeType, String extension) {
        this(uiLabel, mimeType, extension, mimeType);
    }

    ExportFormat(String uiLabel, String mimeType, String extension, String shortMime) {
        this.uiLabel = uiLabel;
        this.mimeType = mimeType;
        this.extension = extension;
        this.shortMime = shortMime;
    }

    public static ExportFormat fromUiLabel(String label) {
        for (ExportFormat format : values()) {
            if (format.uiLabel.equalsIgnoreCase(label)) return format;
        }
        return null;
    }

    public static List<ExportFormat> getFormatsForGoogleMime(GoogleMime googleMimeType) {
        List<ExportFormat> list = new ArrayList<>();
        switch (googleMimeType) {
            case GoogleMime.DOCS -> list.addAll(List.of(DOCX, ODT, PDF, MARKDOWN, TXT, HTML, ZIPHTML, EPUB));
            case GoogleMime.SHEETS -> list.addAll(List.of(XLSX, ODS, PDF, CSV, TSV, HTML, ZIPHTML));
            case GoogleMime.SLIDES -> list.addAll(List.of(PPTX, ODP, PDF, PNG, TXT));
            case GoogleMime.DRAWINGS -> list.addAll(List.of(PNG, JPEG, SVG, PDF));
            case GoogleMime.JAMBOARD -> list.add(PDF);
        }
        return list;
    }

    public static String getFileExtensionFromMimeType(String mimeType) {
        for (ExportFormat format : values()) {
            if (format.mimeType.equalsIgnoreCase(mimeType)) {
                return format.extension;
            }
        }
        return "";
    }

    /**
     * Returns whether the provided ExportFormat results in data loss for multi-sub-document files.
     *
     * @return True if the format is primitive (e.g., CSV, JPG, PNG), false otherwise
     */
    public boolean isPrimitive() {
        return switch (this) {
            case CSV, TSV, PNG, JPEG, SVG -> true;
            default -> false;
        };
    }
}
