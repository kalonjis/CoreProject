package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.FailedGeocodingEntity;
import be.steby.CoreProject.dl.enums.FailedGeocodingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for failed geocoding attempts.
 *
 * Provides queries for:
 * - Finding pending geocoding attempts (for retry)
 * - Counting by status (for monitoring)
 * - Finding existing records to avoid duplicates
 * - Cleanup and stuck record recovery
 *
 * Location: src/main/java/be/steby/CoreProject/dal/repositories/
 */
@Repository
public interface FailedGeocodingRepository extends JpaRepository<FailedGeocodingEntity, Long> {

    /**
     * Finds all geocoding attempts with given status.
     *
     * @param status The status to filter by
     * @return List of failed geocoding records
     */
    List<FailedGeocodingEntity> findByStatus(FailedGeocodingStatus status);

    /**
     * Finds all pending geocoding attempts ordered by creation date.
     * Used by retry scheduler to process oldest first.
     *
     * @param status The status to filter by
     * @return List of failed geocoding records ordered by created_at
     */
    List<FailedGeocodingEntity> findByStatusOrderByCreatedAtAsc(FailedGeocodingStatus status);

    /**
     * Counts failed geocoding attempts by status.
     *
     * Used for monitoring dashboard metrics.
     *
     * @param status The status to count
     * @return Count of records with given status
     */
    long countByStatus(FailedGeocodingStatus status);

    /**
     * Finds a pending failed geocoding record for a specific address.
     *
     * Used to avoid creating duplicate records when the same address
     * fails multiple times while circuit breaker is open.
     *
     * @param address The address to check
     * @param status The status to filter by
     * @return Optional of existing failed geocoding record
     */
    Optional<FailedGeocodingEntity> findByAddressAndStatus(Address address, FailedGeocodingStatus status);

    /**
     * Checks if an address already has a pending geocoding retry.
     *
     * @param address The address to check
     * @param status The status to check for
     * @return true if a pending record exists
     */
    boolean existsByAddressAndStatus(Address address, FailedGeocodingStatus status);

    /**
     * Finds all failed geocoding records for a specific address.
     *
     * Used for debugging and admin dashboard.
     *
     * @param address The address
     * @return List of all failed geocoding attempts for this address
     */
    List<FailedGeocodingEntity> findByAddress(Address address);

    /**
     * Deletes old successful geocoding records.
     *
     * Cleanup query to remove old SUCCESS records and prevent table bloat.
     * Should be called periodically (e.g., daily) via scheduled job.
     *
     * @param status The status to delete (should be SUCCESS)
     * @param cutoff Delete records updated before this date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM FailedGeocodingEntity f WHERE f.status = :status AND f.updatedAt < :cutoff")
    int deleteOldSuccessRecords(@Param("status") FailedGeocodingStatus status,
                                @Param("cutoff") LocalDateTime cutoff);

    /**
     * Resets stuck RETRYING records back to PENDING.
     *
     * Recovery mechanism for records that got stuck in RETRYING status
     * due to application crash or other issues.
     *
     * @param stuckSince Reset records that have been RETRYING since before this time
     * @return Number of reset records
     */
    @Modifying
    @Query("UPDATE FailedGeocodingEntity f SET f.status = 'PENDING', f.errorMessage = 'Reset from stuck RETRYING status' " +
            "WHERE f.status = 'RETRYING' AND f.lastRetryAt < :stuckSince")
    int resetStuckRecords(@Param("stuckSince") LocalDateTime stuckSince);

    /**
     * Deletes all FAILED records (max retries exhausted).
     *
     * Optional cleanup for permanently failed records.
     * Use with caution - may want to keep for audit purposes.
     *
     * @param cutoff Delete records updated before this date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM FailedGeocodingEntity f WHERE f.status = 'FAILED' AND f.updatedAt < :cutoff")
    int deleteOldFailedRecords(@Param("cutoff") LocalDateTime cutoff);
}