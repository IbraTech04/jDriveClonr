package com.ibrasoft.jdriveclonr.model;

import java.util.HashMap;
import java.util.Map;

public class MimeTypeMapping {
    // Google Workspace MIME types
    public static final String DOCS_MIME_TYPE = "application/vnd.google-apps.document";
    public static final String SHEETS_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
    public static final String SLIDES_MIME_TYPE = "application/vnd.google-apps.presentation";
    public static final String DRAWING_MIME_TYPE = "application/vnd.google-apps.drawing";
    public static final String JAMBOARD_MIME_TYPE = "application/vnd.google-apps.jam";

    // Export MIME types
    public static final String DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String PPTX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String PDF_MIME_TYPE = "application/pdf";
    public static final String PNG_MIME_TYPE = "image/png";
    public static final String JPEG_MIME_TYPE = "image/jpeg";
    public static final String SVG_MIME_TYPE = "image/svg+xml";
    public static final String TXT_MIME_TYPE = "text/plain";
    public static final String HTML_MIME_TYPE = "text/html";
    public static final String CSV_MIME_TYPE = "text/csv";

    private static final Map<String, String> uiToMimeType = new HashMap<>();
    private static final Map<String, String> mimeTypeToUi = new HashMap<>();

    static {
        // Initialize UI to MIME type mappings
        initializeMapping("Microsoft Word (.docx)", DOCX_MIME_TYPE);
        initializeMapping("Microsoft Excel (.xlsx)", XLSX_MIME_TYPE);
        initializeMapping("Microsoft PowerPoint (.pptx)", PPTX_MIME_TYPE);
        initializeMapping("PDF (.pdf)", PDF_MIME_TYPE);
        initializeMapping("PNG Image (.png)", PNG_MIME_TYPE);
        initializeMapping("JPEG Image (.jpg)", JPEG_MIME_TYPE);
        initializeMapping("SVG Vector (.svg)", SVG_MIME_TYPE);
        initializeMapping("Plain Text (.txt)", TXT_MIME_TYPE);
        initializeMapping("HTML (.html)", HTML_MIME_TYPE);
        initializeMapping("CSV (.csv)", CSV_MIME_TYPE);
    }

    private static void initializeMapping(String uiValue, String mimeType) {
        uiToMimeType.put(uiValue, mimeType);
        mimeTypeToUi.put(mimeType, uiValue);
    }

    public static String getExportMimeType(String uiValue) {
        return uiToMimeType.get(uiValue);
    }

    public static String getUiValue(String mimeType) {
        return mimeTypeToUi.get(mimeType);
    }

    public static String getDefaultExportMimeType(String googleMimeType) {
        switch (googleMimeType) {
            case DOCS_MIME_TYPE:
                return DOCX_MIME_TYPE;
            case SHEETS_MIME_TYPE:
                return XLSX_MIME_TYPE;
            case SLIDES_MIME_TYPE:
                return PPTX_MIME_TYPE;
            case DRAWING_MIME_TYPE:
                return PNG_MIME_TYPE;
            case JAMBOARD_MIME_TYPE:
                return PDF_MIME_TYPE;
            default:
                return null; // Not a Google Workspace file type
        }
    }

    public static String[] getUiValuesForType(String googleMimeType) {
        switch (googleMimeType) {
            case DOCS_MIME_TYPE:
                return new String[] {
                    "Microsoft Word (.docx)",
                    "PDF (.pdf)",
                    "Plain Text (.txt)",
                    "HTML (.html)"
                };
            case SHEETS_MIME_TYPE:
                return new String[] {
                    "Microsoft Excel (.xlsx)",
                    "PDF (.pdf)",
                    "CSV (.csv)",
                    "HTML (.html)"
                };
            case SLIDES_MIME_TYPE:
                return new String[] {
                    "Microsoft PowerPoint (.pptx)",
                    "PDF (.pdf)",
                    "Plain Text (.txt)",
                    "HTML (.html)"
                };
            case DRAWING_MIME_TYPE:
                return new String[] {
                    "PNG Image (.png)",
                    "JPEG Image (.jpg)",
                    "SVG Vector (.svg)",
                    "PDF (.pdf)"
                };
            case JAMBOARD_MIME_TYPE:
                return new String[] {
                    "PDF (.pdf)",
                    "PNG Image (.png)"
                };
            default:
                return new String[0];
        }
    }
}