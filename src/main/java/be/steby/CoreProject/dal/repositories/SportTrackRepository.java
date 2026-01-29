package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.SportTrack;
import be.steby.CoreProject.dl.enums.SportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SportTrack} entity operations.
 *
 * <p>Provides CRUD operations and domain-specific queries for sport tracks.
 *
 * @see SportTrack
 */
@Repository
public interface SportTrackRepository extends JpaRepository<SportTrack, Long>, JpaSpecificationExecutor<SportTrack> {

    // =========================================================================
    // Basic Lookups
    // =========================================================================

    /**
     * Finds a sport track by its public ID.
     *
     * @param publicId the public UUID
     * @return the sport track if found
     */
    Optional<SportTrack> findByPublicId(String publicId);

    /**
     * Finds a sport track by public ID and owner.
     * Security check to ensure the track belongs to the user.
     *
     * @param publicId      the public UUID
     * @param ownerPublicId owner's public ID
     * @return the sport track if found and owned by user
     */
    Optional<SportTrack> findByPublicIdAndOwnerPublicId(String publicId, String ownerPublicId);

    /**
     * Checks if a sport track exists by public ID.
     *
     * @param publicId the public UUID
     * @return true if exists
     */
    boolean existsByPublicId(String publicId);

    /**
     * Checks if a GPX file has already been imported.
     * Prevents duplicate imports of the same file.
     *
     * @param gpxFilePublicId the GPX file's public ID
     * @return true if already imported
     */
    boolean existsByGpxFilePublicId(String gpxFilePublicId);

    // =========================================================================
    // Owner-Based Queries
    // =========================================================================

    /**
     * Finds all sport tracks for an owner, ordered by start date descending.
     *
     * @param ownerPublicId owner's public ID
     * @return list of sport tracks
     */
    List<SportTrack> findByOwnerPublicIdOrderByStartedAtDesc(String ownerPublicId);

    /**
     * Finds all sport tracks for an owner with pagination.
     *
     * @param ownerPublicId owner's public ID
     * @param pageable      pagination parameters
     * @return page of sport tracks
     */
    Page<SportTrack> findByOwnerPublicId(String ownerPublicId, Pageable pageable);

    /**
     * Finds sport tracks for an owner filtered by sport type.
     *
     * @param ownerPublicId owner's public ID
     * @param sportType     the sport type filter
     * @param pageable      pagination parameters
     * @return page of sport tracks
     */
    Page<SportTrack> findByOwnerPublicIdAndSportType(
            String ownerPublicId,
            SportType sportType,
            Pageable pageable
    );

    /**
     * Finds sport tracks for an owner within a date range.
     *
     * @param ownerPublicId owner's public ID
     * @param startDate     range start (inclusive)
     * @param endDate       range end (inclusive)
     * @param pageable      pagination parameters
     * @return page of sport tracks
     */
    @Query("SELECT st FROM SportTrack st WHERE st.ownerPublicId = :ownerId " +
           "AND st.startedAt >= :startDate AND st.startedAt <= :endDate " +
           "ORDER BY st.startedAt DESC")
    Page<SportTrack> findByOwnerAndDateRange(
            @Param("ownerId") String ownerPublicId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    // =========================================================================
    // Statistics Queries
    // =========================================================================

    /**
     * Counts total tracks for an owner.
     *
     * @param ownerPublicId owner's public ID
     * @return track count
     */
    long countByOwnerPublicId(String ownerPublicId);

    /**
     * Sums total distance for an owner (in meters).
     *
     * @param ownerPublicId owner's public ID
     * @return total distance or null if no tracks
     */
    @Query("SELECT SUM(st.distanceMeters) FROM SportTrack st WHERE st.ownerPublicId = :ownerId")
    Double sumDistanceByOwner(@Param("ownerId") String ownerPublicId);

    /**
     * Sums total duration for an owner (in seconds).
     *
     * @param ownerPublicId owner's public ID
     * @return total duration or null if no tracks
     */
    @Query("SELECT SUM(st.durationSeconds) FROM SportTrack st WHERE st.ownerPublicId = :ownerId")
    Long sumDurationByOwner(@Param("ownerId") String ownerPublicId);

    /**
     * Sums total elevation gain for an owner (in meters).
     *
     * @param ownerPublicId owner's public ID
     * @return total elevation gain or null if no tracks
     */
    @Query("SELECT SUM(st.elevationGain) FROM SportTrack st WHERE st.ownerPublicId = :ownerId")
    Double sumElevationGainByOwner(@Param("ownerId") String ownerPublicId);
}