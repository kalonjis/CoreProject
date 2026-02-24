package be.steby.CoreProject.pl.domains.gdpr.models.responses;

import be.steby.CoreProject.dl.entities.GdprExportRequest;
import be.steby.CoreProject.dl.enums.GdprExportStatus;

/**
 * Response DTO for GDPR export status polling.
 * Returned by {@code GET /api/privacy/export/status}.
 *
 * <p>The frontend uses {@code status} to drive the UI state:
 * <ul>
 *   <li>{@code null}        — no request ever made → show "Request export" button</li>
 *   <li>{@code PENDING}     — confirmation email sent → show "Check your inbox"</li>
 *   <li>{@code PROCESSING}  — archive being generated → show spinner</li>
 *   <li>{@code READY}       — archive ready → show "Download" button</li>
 *   <li>{@code DOWNLOADED}  — already downloaded → show "Request a new export"</li>
 *   <li>{@code EXPIRED}     — link expired → show "Request a new export"</li>
 *   <li>{@code FAILED}      — generation error → show error + "Try again"</li>
 * </ul>
 *
 * @param status        current lifecycle status (null if no request exists)
 * @param requestedAt   when the request was initiated (ISO-8601)
 * @param expiresAt     when the download link expires (ISO-8601, READY status only)
 * @param downloadReady true if the archive can be downloaded right now
 */
public record GdprExportStatusResponse(
        GdprExportStatus status,
        String requestedAt,
        String expiresAt,
        boolean downloadReady
) {

    /** No export request exists for this user. */
    public static GdprExportStatusResponse none() {
        return new GdprExportStatusResponse(null, null, null, false);
    }

    /** Builds a response from an existing export request. */
    public static GdprExportStatusResponse from(GdprExportRequest req) {
        return new GdprExportStatusResponse(
                req.getStatus(),
                req.getCreatedAt() != null ? req.getCreatedAt().toString() : null,
                req.getExpiresAt()  != null ? req.getExpiresAt().toString()  : null,
                req.isDownloadLinkValid()
        );
    }
}