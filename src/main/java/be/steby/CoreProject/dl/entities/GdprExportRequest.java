package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.GdprExportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tracks a user's GDPR data export request through its full lifecycle.
 *
 * <p>One row is created per export request. The {@code downloadToken} is a
 * single-use UUID sent by email — it identifies both the confirmation link
 * and the download link.
 *
 * <p>State machine: {@link GdprExportStatus}
 *
 * <p>Security rules enforced at the service layer:
 * <ul>
 *   <li>Only one active request (PENDING / PROCESSING / READY) per user at a time</li>
 *   <li>Cooldown of N days between requests (configurable)</li>
 *   <li>Download token is single-use and expires after TTL</li>
 * </ul>
 */
@Entity
@Table(
    name = "gdpr_export_request",
    indexes = {
        @Index(name = "idx_gdpr_export_user",          columnList = "user_id"),
        @Index(name = "idx_gdpr_export_download_token", columnList = "download_token", unique = true),
        @Index(name = "idx_gdpr_export_status",         columnList = "status"),
        @Index(name = "idx_gdpr_export_expires_at",     columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(callSuper = true)
public class GdprExportRequest extends BaseEntity<Long> {

    // =========================================================================
    // Ownership
    // =========================================================================

    /**
     * User who requested the export.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    // =========================================================================
    // Status
    // =========================================================================

    /**
     * Current status of the export request.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GdprExportStatus status;

    // =========================================================================
    // Token & Download
    // =========================================================================

    /**
     * Single-use UUID sent by email.
     * Used for both confirmation and download links.
     * Generated at request time, never regenerated.
     */
    @Column(name = "download_token", nullable = false, unique = true,
            updatable = false, length = 36)
    private String downloadToken;

    /**
     * When the download link (and the archive) expires.
     * Set when the archive is ready ({@link GdprExportStatus#READY}).
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    // =========================================================================
    // Timestamps
    // =========================================================================

    /**
     * When the user confirmed the request (clicked the email link).
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * When archive generation completed.
     */
    @Column(name = "generated_at")
    private Instant generatedAt;

    /**
     * When the user downloaded the archive.
     */
    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    // =========================================================================
    // Error tracking
    // =========================================================================

    /**
     * Error message populated when status is {@link GdprExportStatus#FAILED}.
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns true if the download link is still valid.
     */
    public boolean isDownloadLinkValid() {
        return status == GdprExportStatus.READY
            && expiresAt != null
            && Instant.now().isBefore(expiresAt);
    }

    /**
     * Returns true if this request is still active (not terminal).
     */
    public boolean isActive() {
        return status == GdprExportStatus.PENDING
            || status == GdprExportStatus.PROCESSING
            || status == GdprExportStatus.READY;
    }
}