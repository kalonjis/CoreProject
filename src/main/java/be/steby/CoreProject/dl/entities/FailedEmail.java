package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.FailedEmailStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing a failed email queued for retry.
 * 
 * Lifecycle:
 * 1. PENDING  → Email just failed, waiting for retry
 * 2. RETRYING → Currently being processed
 * 3. SENT     → Successfully sent on retry
 * 4. FAILED   → Max retries exceeded, manual intervention needed
 * 
 * A scheduled job picks up PENDING emails and retries them.
 */
@Entity
@Table(name = "failed_email", indexes = {
        @Index(name = "idx_failed_email_status", columnList = "status"),
        @Index(name = "idx_failed_email_next_retry", columnList = "next_retry_at"),
        @Index(name = "idx_failed_email_status_retry", columnList = "status, next_retry_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email subject.
     */
    @Column(nullable = false, length = 500)
    private String subject;

    /**
     * Rendered HTML content.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String htmlContent;

    /**
     * Comma-separated recipient addresses.
     */
    @Column(nullable = false, length = 2000)
    private String recipients;

    /**
     * Current status in the retry lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FailedEmailStatus status = FailedEmailStatus.PENDING;

    /**
     * Number of retry attempts made.
     */
    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /**
     * Maximum retry attempts before marking as FAILED.
     */
    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    /**
     * When the next retry should be attempted.
     */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    /**
     * Reason for the last failure.
     */
    @Column(length = 2000)
    private String lastFailureReason;

    /**
     * Stack trace of the last exception (truncated).
     */
    @Column(columnDefinition = "TEXT")
    private String lastExceptionTrace;

    /**
     * When the email originally failed.
     */
    @Column(name = "original_failure_at", nullable = false)
    private Instant originalFailureAt;

    /**
     * When the email was successfully sent (if status = SENT).
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Increments retry count and calculates next retry time.
     * Uses exponential backoff: 1min, 2min, 4min, 8min, 16min...
     */
    public void incrementRetry() {
        this.retryCount++;
        if (this.retryCount >= this.maxRetries) {
            this.status = FailedEmailStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            long delayMinutes = (long) Math.pow(2, this.retryCount - 1);
            this.nextRetryAt = Instant.now().plusSeconds(delayMinutes * 60);
        }
    }

    /**
     * Marks the email as successfully sent.
     */
    public void markAsSent() {
        this.status = FailedEmailStatus.SENT;
        this.sentAt = Instant.now();
        this.nextRetryAt = null;
    }

    /**
     * Converts recipients string back to array.
     */
    public String[] getRecipientsArray() {
        return recipients.split(",");
    }
}