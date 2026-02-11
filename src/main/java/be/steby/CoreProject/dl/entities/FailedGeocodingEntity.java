package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.FailedGeocodingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a failed geocoding attempt.
 *
 * Unlike SMTP (which stores the entire email content), this entity only stores
 * the Address ID. The full address data is retrieved from the addresses table
 * when retrying.
 *
 * Key differences from FailedEmailEntity:
 * - Only stores address_id (foreign key)
 * - No content duplication
 * - Simpler and more normalized
 *
 * Lifecycle:
 * 1. Created with status PENDING when geocoding fails
 * 2. Status → RETRYING during batch retry
 * 3. Status → SUCCESS if retry succeeds
 * 4. Status → FAILED if all retries exhausted
 *
 * Table: failed_geocoding
 * Location: src/main/java/be/steby/CoreProject/dl/entities/
 */
@Entity
@Table(name = "failed_geocoding", indexes = {
        @Index(name = "idx_failed_geocoding_status", columnList = "status"),
        @Index(name = "idx_failed_geocoding_address_id", columnList = "address_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FailedGeocodingEntity extends BaseEntity<Long> {

    /**
     * Reference to the Address that failed to geocode.
     * We don't need to store address fields - they're already in the addresses table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    /**
     * Current status of the geocoding retry attempt.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FailedGeocodingStatus status;

    /**
     * Number of retry attempts made.
     * Used to limit total retries (e.g., max 5 attempts).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Error message from the last failed geocoding attempt.
     * Helps debug why geocoding is failing.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp of the last retry attempt.
     * Null if no retry has been attempted yet.
     */
    @Column
    private LocalDateTime lastRetryAt;

    // ===========================================================================
    // BUSINESS METHODS
    // ===========================================================================

    /**
     * Checks if this geocoding attempt can be retried.
     *
     * @param maxRetries Maximum number of retry attempts allowed
     * @return true if retry count is below max and status is PENDING
     */
    public boolean canRetry(int maxRetries) {
        return this.status == FailedGeocodingStatus.PENDING
                && this.retryCount < maxRetries;
    }

    /**
     * Marks this geocoding attempt as currently being retried.
     */
    public void markAsRetrying() {
        this.status = FailedGeocodingStatus.RETRYING;
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    /**
     * Marks this geocoding attempt as successful.
     */
    public void markAsSuccess() {
        this.status = FailedGeocodingStatus.SUCCESS;
    }

    /**
     * Marks this geocoding attempt as permanently failed.
     *
     * @param errorMessage Reason for failure
     */
    public void markAsFailed(String errorMessage) {
        this.status = FailedGeocodingStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Resets to PENDING status after a failed retry.
     * Allows the batch job to pick it up again on next run.
     *
     * @param errorMessage Reason for this retry failure
     */
    public void resetToPending(String errorMessage) {
        this.status = FailedGeocodingStatus.PENDING;
        this.errorMessage = errorMessage;
    }
}