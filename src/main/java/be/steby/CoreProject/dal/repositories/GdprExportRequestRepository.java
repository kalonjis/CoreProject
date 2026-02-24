package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.GdprExportRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.GdprExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link GdprExportRequest} entity operations.
 *
 * @see GdprExportRequest
 */
@Repository
public interface GdprExportRequestRepository extends JpaRepository<GdprExportRequest, Long> {

    // =========================================================================
    // Token Lookup
    // =========================================================================

    /**
     * Finds a request by its single-use download token.
     * Used for both confirmation and download endpoints.
     *
     * @param downloadToken the UUID token sent by email
     * @return the export request if found
     */
    Optional<GdprExportRequest> findByDownloadToken(String downloadToken);

    // =========================================================================
    // User Queries
    // =========================================================================

    /**
     * Finds the most recent export request for a user, regardless of status.
     * Used to enforce the cooldown period between requests.
     *
     * @param user the user
     * @return the most recent request if any
     */
    Optional<GdprExportRequest> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds any active request for a user (PENDING / PROCESSING / READY).
     * A user cannot have more than one active request at a time.
     *
     * @param user     the user
     * @param statuses the set of active statuses
     * @return the active request if any
     */
    Optional<GdprExportRequest> findByUserAndStatusIn(User user, List<GdprExportStatus> statuses);

    // =========================================================================
    // Scheduled Cleanup
    // =========================================================================

    /**
     * Finds all READY requests whose archive has expired.
     * Called by the scheduled cleanup job to expire stale archives.
     *
     * @param now current timestamp
     * @return list of expired requests still marked as READY
     */
    @Query("SELECT r FROM GdprExportRequest r WHERE r.status = 'READY' AND r.expiresAt < :now")
    List<GdprExportRequest> findExpiredReadyRequests(@Param("now") Instant now);

    /**
     * Bulk-updates expired READY requests to EXPIRED status.
     * More efficient than loading entities one by one.
     *
     * @param now current timestamp
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE GdprExportRequest r SET r.status = 'EXPIRED' " +
           "WHERE r.status = 'READY' AND r.expiresAt < :now")
    int markExpiredRequests(@Param("now") Instant now);
}