package com.ibrasoft.jdriveclonr.utils;

import com.google.api.client.util.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.regex.Pattern;

public class FileUtils {

    /**
     * Pattern to match invalid characters in Windows filenames.
     * This pattern includes characters that are not allowed in Windows filenames.
     */
    private static final Pattern INVALID_CHARS = Pattern.compile("[<>:\"/\\\\|?*\\x00-\\x1F\\x7F]+");

    /**
     * Pattern to match emojis and other special characters.
     * This pattern is based on the Unicode standard for emojis.
     */
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
            "[" +
                    "\uD83C[\uDF00-\uDFFF]" + // Symbols & Pictographs
                    "\uD83D[\uDC00-\uDE4F]" + // Emoticons
                    "\uD83D[\uDE80-\uDEFF]" + // Transport & Map Symbols
                    "\uD83C[\uDDE0-\uDDFF]" + // Flags
                    "\u2702-\u27B0" +         // Dingbats
//                    "\u24C2-\u1F251" +        // Enclosed characters
                    "]"
    );

    /**
     * Names reserved in Windows that cannot be used as filenames.
     */
    private static final String[] RESERVED_NAMES = {
            "con", "aux", "nul", "prn", "com1", "lpt1", "com2", "lpt2", "com3", "lpt3",
            "com4", "lpt4", "com5", "lpt5", "com6", "lpt6", "com7", "lpt7", "com8", "lpt8", "com9", "lpt9"
    };

    /**
     * Sanitizes a filename by removing invalid characters and emojis, and any illegal characters for Windows.
     * @param filename The original filename to sanitize.
     * @return The sanitized filename.
     * @author Ibrahim Chehab
     */
    public static String sanitizeFilename(String filename) {
        String sanitized = INVALID_CHARS.matcher(filename).replaceAll("");
        sanitized = EMOJI_PATTERN.matcher(sanitized).replaceAll("").trim();

        for (String reserved : RESERVED_NAMES) {
            if (sanitized.equalsIgnoreCase(reserved)) {
                sanitized += "_";
                break;
            }
        }

        return sanitized.trim();
    }

    /**
     * Checks if the current windows installation has long path support enabled.
     * @return true if long path support is enabled (or this is a non-nt OS), false otherwise.
     */
    public static boolean checkWindowsRegistryLongPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            return true;
        }

        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"reg", "query", "HKLM\\SYSTEM\\CurrentControlSet\\Control\\FileSystem", "/v", "LongPathsEnabled"}
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("LongPathsEnabled") && line.trim().endsWith("0x1")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Returns the number of free bytes on the drive that backs the given path.
     */
    public static long getFreeBytes(String pathStr) throws Exception {
        Path path = Paths.get(pathStr);
        FileStore store = Files.getFileStore(path);
        return (store.getUsableSpace());
    }

    /**
     * Convenience helper that formats the result nicely (MiB / GiB, etc.).
     */


    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    /**
     * Sets the last modified time of a file to match the given Google DateTime.
     *
     * @param filePath The file to update.
     * @param dateTime The Google API client DateTime.
     * @throws IOException If setting the file time fails.
     */
    public static void setLastModifiedFromDateTime(java.io.File filePath, DateTime dateTime) throws IOException {
        if (!filePath.exists()) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        Path path = filePath.toPath();
        FileTime fileTime = FileTime.fromMillis(dateTime.getValue());
        Files.setLastModifiedTime(path, fileTime);
    }

    public static void setLastModifiedFromDateTime(Path filePath, DateTime dateTime) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        FileTime fileTime = FileTime.fromMillis(dateTime.getValue());
        Files.setLastModifiedTime(filePath, fileTime);
    }

    public static void setLastModifiedFromDateTime(String filePath, DateTime dateTime) throws IOException {
        setLastModifiedFromDateTime(Paths.get(filePath), dateTime);
    }

    public static String getCurrentDateTimeFormatted() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
    }
}
