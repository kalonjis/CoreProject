package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.FailedGeocodingEntity;
import be.steby.CoreProject.dl.enums.FailedGeocodingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for failed geocoding attempts.
 * 
 * Provides queries for:
 * - Finding pending geocoding attempts (for retry)
 * - Counting by status (for monitoring)
 * - Finding existing records to avoid duplicates
 * 
 * Location: src/main/java/be/steby/CoreProject/dl/repositories/
 */
@Repository
public interface FailedGeocodingRepository extends JpaRepository<FailedGeocodingEntity, Long> {

    /**
     * Finds all pending geocoding attempts that are ready for retry.
     * 
     * Used by GeocodingRetryService to batch process failed geocoding attempts.
     * 
     * @return List of failed geocoding records with status PENDING
     */
    List<FailedGeocodingEntity> findByStatus(FailedGeocodingStatus status);

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
     * Deletes all SUCCESS records older than a certain date.
     * 
     * Cleanup query to remove old successful retry records.
     * Should be called periodically (e.g., monthly) to prevent table bloat.
     * 
     * @param cutoffDate Date before which to delete SUCCESS records
     * @return Number of records deleted
     */
    @Query("DELETE FROM FailedGeocodingEntity f WHERE f.status = 'SUCCESS' AND f.updatedAt < :cutoffDate")
    int deleteSuccessRecordsOlderThan(java.time.LocalDateTime cutoffDate);
}