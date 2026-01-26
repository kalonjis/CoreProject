package be.steby.CoreProject.bll.domains.storage.models;

import be.steby.CoreProject.dl.enums.FileCategory;
import lombok.Builder;
import lombok.Getter;

/**
 * Result of a successful file upload operation.
 *
 * <p>Immutable record returned by {@code FileStorageService.store()}.
 * Contains all information needed by calling services.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * FileUploadResult result = fileStorageService.store(file, category, ownerId);
 * String fileId = result.getPublicId();  // Store this reference
 * String url = result.getAccessUrl();    // Use for display
 * }
 * </pre>
 *
 * @see be.steby.CoreProject.bll.domains.storage.services.FileStorageService
 */
@Getter
@Builder
public class FileUploadResult {

    /**
     * Public ID of the stored file.
     * Use this to reference the file in other entities.
     */
    private final String publicId;

    /**
     * Original filename as uploaded.
     */
    private final String originalFilename;

    /**
     * File size in bytes.
     */
    private final long fileSize;

    /**
     * Detected MIME type.
     */
    private final String contentType;

    /**
     * File category.
     */
    private final FileCategory category;

    /**
     * URL to access the file.
     * For public files: direct URL.
     * For private files: URL requiring authentication.
     */
    private final String accessUrl;

    /**
     * SHA-256 checksum (if enabled).
     */
    private final String checksum;

    /**
     * Image width in pixels (null for non-images).
     */
    private final Integer imageWidth;

    /**
     * Image height in pixels (null for non-images).
     */
    private final Integer imageHeight;

    /**
     * Checks if the uploaded file is an image.
     *
     * @return true if content type starts with "image/"
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Returns formatted file size for display.
     *
     * @return human-readable size (e.g., "2.5 MB")
     */
    public String getFormattedSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}