package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.FailedSmsStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a failed SMS queued for retry.
 *
 * Lifecycle:
 * 1. PENDING  → SMS just failed, waiting for retry
 * 2. RETRYING → Currently being processed
 * 3. SENT     → Successfully sent on retry
 * 4. FAILED   → Max retries exceeded, manual intervention needed
 *
 * A scheduled job picks up PENDING SMS and retries them.
 *
 * @see FailedSmsStatus
 * @see be.steby.CoreProject.il.sms.fallback.FailedSmsRetryScheduler
 */
@Entity
@Table(name = "failed_sms", indexes = {
        @Index(name = "idx_failed_sms_status", columnList = "status"),
        @Index(name = "idx_failed_sms_next_retry", columnList = "next_retry_at"),
        @Index(name = "idx_failed_sms_status_retry", columnList = "status, next_retry_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FailedSms extends BaseEntity<Long> {

    /**
     * SMS text content.
     */
    @Column(nullable = false, length = 1600)
    private String content;

    /**
     * Recipient phone number (E.164 format).
     * Unlike email, SMS has a single recipient per message.
     */
    @Column(nullable = false, length = 20)
    private String recipient;

    /**
     * Current status in the retry lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FailedSmsStatus status = FailedSmsStatus.PENDING;

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
     * When the SMS originally failed.
     */
    @Column(name = "original_failure_at", nullable = false)
    private Instant originalFailureAt;

    /**
     * When the SMS was successfully sent (if status = SENT).
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Increments retry count and calculates next retry time.
     * Uses exponential backoff: 1min, 2min, 4min, 8min, 16min...
     */
    public void incrementRetry() {
        this.retryCount++;
        if (this.retryCount >= this.maxRetries) {
            this.status = FailedSmsStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            long delayMinutes = (long) Math.pow(2, this.retryCount - 1);
            this.nextRetryAt = Instant.now().plusSeconds(delayMinutes * 60);
        }
    }

    /**
     * Marks the SMS as successfully sent.
     */
    public void markAsSent() {
        this.status = FailedSmsStatus.SENT;
        this.sentAt = Instant.now();
        this.nextRetryAt = null;
    }

    /**
     * Returns a masked version of the recipient for logging.
     *
     * @return masked phone number (e.g., "+32475****")
     */
    public String getMaskedRecipient() {
        if (recipient == null || recipient.length() < 6) {
            return "****";
        }
        return recipient.substring(0, 6) + "****";
    }

    /**
     * This entity doesn't need a public ID since it's internal infrastructure.
     */
    @Override
    protected boolean shouldGeneratePublicId() {
        return false;
    }
}