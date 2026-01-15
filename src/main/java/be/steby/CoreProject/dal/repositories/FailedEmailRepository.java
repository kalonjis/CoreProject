package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.FailedEmail;
import be.steby.CoreProject.dl.enums.FailedEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for managing failed email persistence and retry operations.
 */
@Repository
public interface FailedEmailRepository extends JpaRepository<FailedEmail, Long> {

    /**
     * Finds emails ready for retry (PENDING status and nextRetryAt has passed).
     * Orders by nextRetryAt to process oldest first.
     * 
     * @param now current timestamp
     * @param limit maximum number of emails to fetch
     * @return list of emails ready for retry
     */
    @Query("""
        SELECT fe FROM FailedEmail fe
        WHERE fe.status = 'PENDING'
          AND fe.nextRetryAt <= :now
        ORDER BY fe.nextRetryAt ASC
        LIMIT :limit
        """)
    List<FailedEmail> findEmailsReadyForRetry(
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    /**
     * Atomically marks an email as RETRYING to prevent concurrent processing.
     * Returns the number of rows updated (1 if successful, 0 if already taken).
     * 
     * @param id email ID
     * @param expectedStatus expected current status (PENDING)
     * @return number of rows updated
     */
    @Modifying
    @Query("""
        UPDATE FailedEmail fe
        SET fe.status = 'RETRYING', fe.updatedAt = :now
        WHERE fe.id = :id AND fe.status = :expectedStatus
        """)
    int markAsRetrying(
            @Param("id") Long id,
            @Param("expectedStatus") FailedEmailStatus expectedStatus,
            @Param("now") Instant now
    );

    /**
     * Counts emails by status for monitoring/metrics.
     * 
     * @param status the status to count
     * @return count of emails with given status
     */
    long countByStatus(FailedEmailStatus status);

    /**
     * Finds all failed emails (terminal failure state) for admin review.
     * 
     * @return list of permanently failed emails
     */
    List<FailedEmail> findByStatusOrderByCreatedAtDesc(FailedEmailStatus status);

    /**
     * Deletes old successfully sent emails for cleanup.
     * 
     * @param status SENT status
     * @param before delete emails sent before this timestamp
     * @return number of deleted records
     */
    @Modifying
    @Query("""
        DELETE FROM FailedEmail fe
        WHERE fe.status = :status AND fe.sentAt < :before
        """)
    int deleteOldSentEmails(
            @Param("status") FailedEmailStatus status,
            @Param("before") Instant before
    );

    /**
     * Resets stuck RETRYING emails back to PENDING.
     * Useful for recovery after application crash.
     * 
     * @param stuckSince emails stuck in RETRYING since before this time
     * @return number of reset records
     */
    @Modifying
    @Query("""
        UPDATE FailedEmail fe
        SET fe.status = 'PENDING', fe.updatedAt = :now
        WHERE fe.status = 'RETRYING' AND fe.updatedAt < :stuckSince
        """)
    int resetStuckEmails(
            @Param("stuckSince") Instant stuckSince,
            @Param("now") Instant now
    );
}