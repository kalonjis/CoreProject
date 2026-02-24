package be.steby.CoreProject.dl.enums;

/**
 * Lifecycle status of a GDPR data export request.
 *
 * <p>State transitions:
 * <pre>
 * PENDING → PROCESSING → READY → DOWNLOADED
 *                              ↘ EXPIRED
 *         ↘ FAILED
 * </pre>
 */
public enum GdprExportStatus {

    /**
     * Export requested — confirmation email sent, awaiting user click.
     */
    PENDING,

    /**
     * Archive is being generated asynchronously.
     */
    PROCESSING,

    /**
     * Archive is ready — download link sent by email.
     */
    READY,

    /**
     * Archive has been downloaded by the user.
     * File is deleted from storage immediately after.
     */
    DOWNLOADED,

    /**
     * Archive TTL expired before the user downloaded it.
     * File has been deleted from storage.
     */
    EXPIRED,

    /**
     * Generation failed due to an internal error.
     */
    FAILED
}