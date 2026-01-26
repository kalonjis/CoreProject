package be.steby.CoreProject.dl.enums;

/**
 * Lifecycle status of a stored file.
 *
 * <p>Tracks the file through its lifecycle from upload to deletion.
 * Used for soft-delete functionality and file management.
 *
 * <p>Typical lifecycle:
 * <pre>
 * PENDING → ACTIVE → ARCHIVED → DELETED
 *                  ↘ DELETED (direct delete)
 * </pre>
 *
 * @see be.steby.CoreProject.dl.entities.StoredFile
 */
public enum FileStatus {

    /**
     * File uploaded but not yet processed/validated.
     * Used for async processing workflows.
     */
    PENDING,

    /**
     * File is active and accessible.
     * Normal operational state.
     */
    ACTIVE,

    /**
     * File is archived (soft-deleted by user).
     * Can be restored. Not accessible via normal queries.
     */
    ARCHIVED,

    /**
     * File marked for deletion.
     * Physical file will be removed by scheduled cleanup job.
     */
    DELETED
}