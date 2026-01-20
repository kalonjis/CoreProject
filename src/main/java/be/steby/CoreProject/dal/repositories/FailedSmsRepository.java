package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.FailedSms;
import be.steby.CoreProject.dl.enums.FailedSmsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for managing failed SMS persistence and retry operations.
 *
 * @see FailedSms
 * @see be.steby.CoreProject.il.sms.fallback.FailedSmsRetryScheduler
 */
@Repository
public interface FailedSmsRepository extends JpaRepository<FailedSms, Long> {

    /**
     * Finds SMS ready for retry (PENDING status and nextRetryAt has passed).
     * Orders by nextRetryAt to process oldest first.
     *
     * @param now   current timestamp
     * @param limit maximum number of SMS to fetch
     * @return list of SMS ready for retry
     */
    @Query("""
            SELECT fs FROM FailedSms fs
            WHERE fs.status = 'PENDING'
              AND fs.nextRetryAt <= :now
            ORDER BY fs.nextRetryAt ASC
            LIMIT :limit
            """)
    List<FailedSms> findSmsReadyForRetry(
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    /**
     * Atomically marks an SMS as RETRYING to prevent concurrent processing.
     * Returns the number of rows updated (1 if successful, 0 if already taken).
     *
     * @param id             SMS ID
     * @param expectedStatus expected current status (PENDING)
     * @param now            current timestamp
     * @return number of rows updated
     */
    @Modifying
    @Query("""
            UPDATE FailedSms fs
            SET fs.status = 'RETRYING', fs.updatedAt = :now
            WHERE fs.id = :id AND fs.status = :expectedStatus
            """)
    int markAsRetrying(
            @Param("id") Long id,
            @Param("expectedStatus") FailedSmsStatus expectedStatus,
            @Param("now") Instant now
    );

    /**
     * Counts SMS by status for monitoring/metrics.
     *
     * @param status the status to count
     * @return count of SMS with given status
     */
    long countByStatus(FailedSmsStatus status);

    /**
     * Finds all failed SMS (terminal failure state) for admin review.
     *
     * @param status the status to filter by
     * @return list of permanently failed SMS
     */
    List<FailedSms> findByStatusOrderByCreatedAtDesc(FailedSmsStatus status);

    /**
     * Deletes old successfully sent SMS for cleanup.
     *
     * @param status SENT status
     * @param before delete SMS sent before this timestamp
     * @return number of deleted records
     */
    @Modifying
    @Query("""
            DELETE FROM FailedSms fs
            WHERE fs.status = :status AND fs.sentAt < :before
            """)
    int deleteOldSentSms(
            @Param("status") FailedSmsStatus status,
            @Param("before") Instant before
    );

    /**
     * Resets stuck RETRYING SMS back to PENDING.
     * Useful for recovery after application crash.
     *
     * @param stuckSince SMS stuck in RETRYING since before this time
     * @param now        current timestamp
     * @return number of reset records
     */
    @Modifying
    @Query("""
            UPDATE FailedSms fs
            SET fs.status = 'PENDING', fs.updatedAt = :now
            WHERE fs.status = 'RETRYING' AND fs.updatedAt < :stuckSince
            """)
    int resetStuckSms(
            @Param("stuckSince") Instant stuckSince,
            @Param("now") Instant now
    );
}