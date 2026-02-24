package be.steby.CoreProject.bll.domains.gdpr.services.data_export;

import be.steby.CoreProject.bll.domains.gdpr.models.GdprDownloadResult;
import be.steby.CoreProject.dl.entities.GdprExportRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.bll.domains.gdpr.exceptions.GdprExportException;

import java.util.Optional;

/**
 * Orchestrates the full GDPR data export lifecycle.
 *
 * <p>Flow:
 * <pre>
 * 1. request(user)           → creates GdprExportRequest (PENDING)
 *                               sends confirmation email
 * 2. confirm(token)          → validates token, triggers async generation (PROCESSING)
 * 3. generateArchiveAsync()  → @Async: collects data, builds ZIP, stores it (READY)
 *                               sends download-ready email
 * 4. download(token)         → serves file (local) or redirects 302 (R2)
 *                               marks request as DOWNLOADED, deletes archive
 * 5. getStatus(user)         → returns current request for frontend polling
 * </pre>
 */
public interface GdprExportService {

    /**
     * Initiates an export request for the given user.
     *
     * <p>Enforces:
     * <ul>
     *   <li>No active request already in progress</li>
     *   <li>Cooldown period between requests</li>
     * </ul>
     *
     * <p>On success: persists a PENDING request and sends a confirmation email.
     *
     * @param user the authenticated user requesting the export
     * @return the created export request
     * @throws GdprExportException if a request is already active or cooldown not elapsed
     */
    GdprExportRequest request(User user);

    /**
     * Confirms the export request via the token received by email.
     * Triggers asynchronous archive generation.
     *
     * @param token the single-use download token from the confirmation email
     * @throws GdprExportException if token is invalid, expired, or already confirmed
     */
    void confirm(String token);

    /**
     * Generates the ZIP archive asynchronously.
     * Called internally after confirmation — do not call directly.
     *
     * @param requestId the internal ID of the GdprExportRequest to process
     */
    void generateArchiveAsync(Long requestId);

    /**
     * Serves the archive or returns a pre-signed redirect URL.
     *
     * <p>Behavior depends on the active storage provider:
     * <ul>
     *   <li>Local: returns the raw bytes for the controller to stream</li>
     *   <li>R2: returns a pre-signed URL for HTTP 302 redirect</li>
     * </ul>
     *
     * <p>After a successful download: marks request as DOWNLOADED and deletes the archive.
     *
     * @param token the single-use download token
     * @return download result containing either raw bytes (local) or redirect URL (R2)
     * @throws GdprExportException if token is invalid, expired, or already downloaded
     */
    GdprDownloadResult download(String token);

    /**
     * Returns the most recent export request for the user.
     * Used by the frontend to poll for status updates.
     *
     * @param user the authenticated user
     * @return the most recent request, or empty if none exists
     */
    Optional<GdprExportRequest> getStatus(User user);
}