package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.SportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;

/**
 * Entity representing a recorded sport track/session.
 *
 * <p>Stores metadata extracted from GPX files along with calculated metrics.
 * The original GPX file is referenced via {@code gpxFilePublicId} and stored
 * separately in the file storage system.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Extends BaseEntity for publicId, auditing</li>
 *   <li>GPX file reference via publicId (not FK) for decoupling</li>
 *   <li>Simplified track points stored as JSON for map display</li>
 *   <li>All metrics pre-calculated at import time for fast reads</li>
 * </ul>
 *
 * @see SportType
 */
@Entity
@Table(name = "sport_track", indexes = {
        @Index(name = "idx_sport_track_public_id", columnList = "public_id"),
        @Index(name = "idx_sport_track_owner", columnList = "owner_public_id"),
        @Index(name = "idx_sport_track_type", columnList = "sport_type"),
        @Index(name = "idx_sport_track_started_at", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class SportTrack extends BaseEntity<Long> {

    // =========================================================================
    // Ownership & Source
    // =========================================================================

    /**
     * Public ID of the activity owner (User).
     * Using publicId instead of FK for flexibility and security.
     */
    @Column(name = "owner_public_id", nullable = false, length = 256)
    private String ownerPublicId;

    /**
     * Public ID of the source GPX file in storage.
     * Allows retrieval of original file for re-processing if needed.
     */
    @Column(name = "gpx_file_public_id", nullable = false, length = 256)
    private String gpxFilePublicId;

    // =========================================================================
    // Activity Metadata
    // =========================================================================

    /**
     * User-defined or GPX-extracted activity name.
     * Example: "Sortie VTT - Part II"
     */
    @Column(name = "name", nullable = false, length = 255)
    @ToString.Include
    private String name;

    /**
     * Type of sport (cycling, running, hiking, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 50)
    @ToString.Include
    private SportType sportType;

    /**
     * Timestamp when the activity started.
     * Extracted from first trackpoint's time element.
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /**
     * Timestamp when the activity ended.
     * Extracted from last trackpoint's time element.
     */
    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    // =========================================================================
    // Calculated Metrics
    // =========================================================================

    /**
     * Total distance in meters.
     * Calculated using Haversine formula between consecutive points.
     */
    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    /**
     * Total duration in seconds.
     * Calculated as endedAt - startedAt.
     */
    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    /**
     * Average speed in meters per second.
     * Calculated as distance / duration.
     */
    @Column(name = "avg_speed_mps")
    private Double avgSpeedMps;

    /**
     * Maximum speed in meters per second.
     * Calculated from segment speeds.
     */
    @Column(name = "max_speed_mps")
    private Double maxSpeedMps;

    /**
     * Minimum elevation in meters.
     */
    @Column(name = "elevation_min")
    private Double elevationMin;

    /**
     * Maximum elevation in meters.
     */
    @Column(name = "elevation_max")
    private Double elevationMax;

    /**
     * Total elevation gain (D+) in meters.
     * Sum of positive elevation changes with smoothing applied.
     */
    @Column(name = "elevation_gain")
    private Double elevationGain;

    /**
     * Total elevation loss (D-) in meters.
     * Sum of negative elevation changes with smoothing applied.
     */
    @Column(name = "elevation_loss")
    private Double elevationLoss;

    // =========================================================================
    // Track Points (for map display)
    // =========================================================================

    /**
     * Total number of track points in the original GPX.
     */
    @Column(name = "total_track_points", nullable = false)
    private Integer totalTrackPoints;

    /**
     * Simplified track points as JSON array for map display.
     * Format: [[lat, lon, ele], [lat, lon, ele], ...]
     * Reduced using Douglas-Peucker algorithm to ~500 points max.
     */
    @Column(name = "simplified_track")
    @Lob
    private String simplifiedTrack;

    /**
     * Elevation profile as JSON array for chart display.
     * Format: [[distanceKm, elevationM], [distanceKm, elevationM], ...]
     */
    @Column(name = "elevation_profile")
    @Lob
    private String elevationProfile;

    // =========================================================================
    // Bounding Box (for map centering)
    // =========================================================================

    /**
     * Minimum latitude of the track.
     */
    @Column(name = "bounds_min_lat")
    private Double boundsMinLat;

    /**
     * Maximum latitude of the track.
     */
    @Column(name = "bounds_max_lat")
    private Double boundsMaxLat;

    /**
     * Minimum longitude of the track.
     */
    @Column(name = "bounds_min_lon")
    private Double boundsMinLon;

    /**
     * Maximum longitude of the track.
     */
    @Column(name = "bounds_max_lon")
    private Double boundsMaxLon;

    // =========================================================================
    // Convenience Methods
    // =========================================================================

    /**
     * Gets the duration as a Java Duration object.
     */
    public Duration getDuration() {
        return Duration.ofSeconds(durationSeconds);
    }

    /**
     * Gets the distance in kilometers.
     */
    public double getDistanceKm() {
        return distanceMeters / 1000.0;
    }

    /**
     * Gets the average speed in km/h.
     */
    public double getAvgSpeedKmh() {
        return avgSpeedMps != null ? avgSpeedMps * 3.6 : 0.0;
    }

    /**
     * Gets the max speed in km/h.
     */
    public double getMaxSpeedKmh() {
        return maxSpeedMps != null ? maxSpeedMps * 3.6 : 0.0;
    }

    /**
     * Gets the center point of the bounding box.
     *
     * @return array [lat, lon] of center point
     */
    public double[] getBoundsCenter() {
        if (boundsMinLat == null || boundsMaxLat == null || 
            boundsMinLon == null || boundsMaxLon == null) {
            return null;
        }
        return new double[] {
            (boundsMinLat + boundsMaxLat) / 2,
            (boundsMinLon + boundsMaxLon) / 2
        };
    }
}