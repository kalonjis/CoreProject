package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.FileCategory;
import be.steby.CoreProject.dl.enums.FileStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing metadata for a stored file.
 *
 * <p>This entity stores only metadata. The actual file content
 * is stored on the filesystem (or cloud storage in the future).
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Extends BaseEntity for publicId, auditing</li>
 *   <li>Original filename preserved for user display</li>
 *   <li>Storage path is internal, never exposed via API</li>
 *   <li>Owner tracked via publicId (not FK) for flexibility</li>
 * </ul>
 *
 * <p>Security: The publicId is used in URLs, never the internal path.
 *
 * @see FileCategory
 * @see FileStatus
 */
@Entity
@Table(name = "stored_file", indexes = {
        @Index(name = "idx_stored_file_public_id", columnList = "public_id"),
        @Index(name = "idx_stored_file_owner", columnList = "owner_public_id"),
        @Index(name = "idx_stored_file_category", columnList = "category"),
        @Index(name = "idx_stored_file_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class StoredFile extends BaseEntity<Long> {

    // =========================================================================
    // File Identification
    // =========================================================================

    /**
     * Original filename as uploaded by user.
     * Preserved for display purposes only.
     * Never used for storage (security risk).
     */
    @Column(name = "original_filename", nullable = false, length = 255)
    @ToString.Include
    private String originalFilename;

    /**
     * Generated filename used for storage.
     * Format: {uuid}.{extension}
     * Prevents path traversal and naming conflicts.
     */
    @Column(name = "stored_filename", nullable = false, length = 100)
    private String storedFilename;

    /**
     * Relative path from storage base directory.
     * Format: {category}/{year}/{month}/{stored_filename}
     * Example: avatars/2025/01/abc123.jpg
     */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    // =========================================================================
    // File Properties
    // =========================================================================

    /**
     * MIME type of the file.
     * Detected from file content, not from extension.
     * Example: image/jpeg, application/pdf
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * File size in bytes.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * File extension (lowercase, without dot).
     * Example: jpg, pdf, png
     */
    @Column(name = "extension", length = 20)
    private String extension;

    // =========================================================================
    // Classification
    // =========================================================================

    /**
     * Category determines validation rules and storage location.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private FileCategory category;

    /**
     * Current lifecycle status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FileStatus status = FileStatus.ACTIVE;

    // =========================================================================
    // Ownership
    // =========================================================================

    /**
     * Public ID of the owner (User, Product, etc.).
     * Using String instead of FK for flexibility across domains.
     */
    @Column(name = "owner_public_id", nullable = false, length = 256)
    private String ownerPublicId;

    /**
     * Type of owner entity.
     * Example: USER, PRODUCT, COMPANY
     * Helps identify the owner domain.
     */
    @Column(name = "owner_type", nullable = false, length = 50)
    private String ownerType;

    // =========================================================================
    // Optional Metadata
    // =========================================================================

    /**
     * SHA-256 hash of file content.
     * Used for duplicate detection and integrity verification.
     */
    @Column(name = "checksum", length = 64)
    private String checksum;

    /**
     * Image width in pixels (null for non-images).
     */
    @Column(name = "image_width")
    private Integer imageWidth;

    /**
     * Image height in pixels (null for non-images).
     */
    @Column(name = "image_height")
    private Integer imageHeight;

    /**
     * Optional description or alt text.
     */
    @Column(name = "description", length = 500)
    private String description;

    // =========================================================================
    // Soft Delete Support
    // =========================================================================

    /**
     * Timestamp when file was marked for deletion.
     * Physical deletion happens via scheduled job.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // =========================================================================
    // Convenience Methods
    // =========================================================================

    /**
     * Checks if file is publicly accessible.
     */
    public boolean isPublic() {
        return category != null && category.isPublicAccess();
    }

    /**
     * Checks if file is an image.
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Checks if file is active and accessible.
     */
    public boolean isAccessible() {
        return status == FileStatus.ACTIVE;
    }

    /**
     * Marks file for deletion (soft delete).
     */
    public void markDeleted() {
        this.status = FileStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    /**
     * Returns human-readable file size.
     */
    public String getFormattedSize() {
        if (fileSize == null) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}