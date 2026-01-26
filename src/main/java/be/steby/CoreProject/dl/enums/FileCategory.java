package be.steby.CoreProject.dl.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of file categories supported by the storage system.
 *
 * <p>Each category defines specific validation rules and storage behavior:
 * <ul>
 *   <li>Allowed MIME types</li>
 *   <li>Maximum file size</li>
 *   <li>Storage subdirectory</li>
 *   <li>Public/private access</li>
 * </ul>
 *
 * <p>Categories are used by validators to apply appropriate rules
 * and by the storage service to organize files.
 *
 * @see be.steby.CoreProject.bll.domains.storage.validators.FileValidator
 */
@Getter
@RequiredArgsConstructor
public enum FileCategory {

    /**
     * User profile pictures.
     * Typically small images, publicly accessible.
     */
    AVATAR("avatars", true),

    /**
     * User-uploaded documents (ID, contracts, etc.).
     * Private, requires authentication to access.
     */
    DOCUMENT("documents", false),

    /**
     * Product images for catalog.
     * Publicly accessible for display.
     */
    PRODUCT_IMAGE("products", true),

    /**
     * Temporary files pending processing.
     * Private, auto-deleted after processing.
     */
    TEMPORARY("temp", false);

    /**
     * Subdirectory name for this category.
     * Files are organized as: {base-path}/{subdirectory}/{year}/{month}/{filename}
     */
    private final String subdirectory;

    /**
     * Whether files in this category are publicly accessible.
     * Public files can be served without authentication.
     * Private files require valid user session.
     */
    private final boolean publicAccess;
}