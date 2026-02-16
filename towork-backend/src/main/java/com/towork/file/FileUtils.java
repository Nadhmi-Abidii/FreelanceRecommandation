package com.towork.file;

import java.util.Arrays;
import java.util.List;

public class FileUtils {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp");
    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx", ".txt", ".rtf", ".zip", ".rar", ".7z");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex).toLowerCase();
    }

    public static boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension);
    }

    public static boolean isDocumentFile(String filename) {
        String extension = getFileExtension(filename);
        return ALLOWED_DOCUMENT_EXTENSIONS.contains(extension);
    }

    public static boolean isValidFileSize(long fileSize) {
        return fileSize <= MAX_FILE_SIZE;
    }

    public static boolean isValidFileType(String filename) {
        return isImageFile(filename) || isDocumentFile(filename);
    }

    public static String getFileType(String filename) {
        if (isImageFile(filename)) {
            return "IMAGE";
        } else if (isDocumentFile(filename)) {
            return "DOCUMENT";
        }
        return "UNKNOWN";
    }
}
