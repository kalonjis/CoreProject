package be.steby.CoreProject.bll.domains.sport.services;

import be.steby.CoreProject.bll.domains.sport.models.GpxParseResult;
import be.steby.CoreProject.dl.entities.SportTrack;
import be.steby.CoreProject.dl.enums.SportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service for managing sport tracks.
 *
 * <p>Orchestrates GPX import and provides CRUD operations for sport tracks.
 * Handles the full lifecycle from GPX upload to track retrieval.
 *
 * @see SportTrack
 * @see GpxParsingService
 */
public interface SportTrackService {

    // =========================================================================
    // Import
    // =========================================================================

    /**
     * Imports a GPX file and creates a new sport track.
     *
     * <p>Flow:
     * <ol>
     *   <li>Parse GPX file from storage</li>
     *   <li>Calculate all metrics</li>
     *   <li>Create and persist SportTrack entity</li>
     * </ol>
     *
     * @param gpxFilePublicId public ID of the uploaded GPX file
     * @param ownerPublicId   public ID of the track owner
     * @param sportType       type of sport activity
     * @return the created sport track
     * @throws GpxAlreadyImportedException if GPX file was already imported
     * @throws GpxParsingException         if parsing fails
     */
    SportTrack importFromGpx(String gpxFilePublicId, String ownerPublicId, SportType sportType);

    /**
     * Imports a GPX file with auto-detected sport type.
     * Defaults to OTHER if type cannot be determined.
     *
     * @param gpxFilePublicId public ID of the uploaded GPX file
     * @param ownerPublicId   public ID of the track owner
     * @return the created sport track
     */
    SportTrack importFromGpx(String gpxFilePublicId, String ownerPublicId);

    // =========================================================================
    // Read Operations
    // =========================================================================

    /**
     * Finds a sport track by its public ID.
     *
     * @param publicId the track's public ID
     * @return the sport track if found
     */
    Optional<SportTrack> findByPublicId(String publicId);

    /**
     * Finds a sport track by public ID, ensuring ownership.
     *
     * @param publicId      the track's public ID
     * @param ownerPublicId the expected owner's public ID
     * @return the sport track if found and owned by user
     */
    Optional<SportTrack> findByPublicIdAndOwner(String publicId, String ownerPublicId);

    /**
     * Gets a sport track by public ID or throws exception.
     *
     * @param publicId the track's public ID
     * @return the sport track
     * @throws SportTrackNotFoundException if not found
     */
    SportTrack getByPublicId(String publicId);

    /**
     * Gets a sport track by public ID, ensuring ownership.
     *
     * @param publicId      the track's public ID
     * @param ownerPublicId the expected owner's public ID
     * @return the sport track
     * @throws SportTrackNotFoundException if not found or not owned
     */
    SportTrack getByPublicIdAndOwner(String publicId, String ownerPublicId);

    /**
     * Lists all sport tracks for an owner with pagination.
     *
     * @param ownerPublicId owner's public ID
     * @param pageable      pagination parameters
     * @return page of sport tracks
     */
    Page<SportTrack> findByOwner(String ownerPublicId, Pageable pageable);

    /**
     * Lists sport tracks for an owner filtered by sport type.
     *
     * @param ownerPublicId owner's public ID
     * @param sportType     sport type filter
     * @param pageable      pagination parameters
     * @return page of sport tracks
     */
    Page<SportTrack> findByOwnerAndType(String ownerPublicId, SportType sportType, Pageable pageable);

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Updates a sport track's metadata.
     *
     * @param publicId      the track's public ID
     * @param ownerPublicId owner's public ID (for authorization)
     * @param name          new name (null to keep existing)
     * @param sportType     new sport type (null to keep existing)
     * @return the updated sport track
     * @throws SportTrackNotFoundException if not found or not owned
     */
    SportTrack update(String publicId, String ownerPublicId, String name, SportType sportType);

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Deletes a sport track.
     *
     * @param publicId      the track's public ID
     * @param ownerPublicId owner's public ID (for authorization)
     * @throws SportTrackNotFoundException if not found or not owned
     */
    void delete(String publicId, String ownerPublicId);

    // =========================================================================
    // Statistics
    // =========================================================================

    /**
     * Gets aggregated statistics for an owner.
     *
     * @param ownerPublicId owner's public ID
     * @return statistics summary
     */
    OwnerStats getOwnerStats(String ownerPublicId);

    /**
     * Owner statistics record.
     */
    record OwnerStats(
            long totalTracks,
            double totalDistanceKm,
            long totalDurationSeconds,
            double totalElevationGainM
    ) {
        public double getTotalDurationHours() {
            return totalDurationSeconds / 3600.0;
        }
    }
}