package be.steby.CoreProject.bll.domains.sport.models;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Result of parsing a GPX file.
 *
 * <p>Contains all extracted data and calculated metrics ready
 * to be persisted as a {@link be.steby.CoreProject.dl.entities.SportTrack}.
 */
@Getter
@Builder
public class GpxParseResult {

    // =========================================================================
    // Metadata
    // =========================================================================

    /**
     * Track name from GPX metadata or filename.
     */
    private final String name;

    /**
     * Start timestamp (first trackpoint time).
     */
    private final Instant startedAt;

    /**
     * End timestamp (last trackpoint time).
     */
    private final Instant endedAt;

    // =========================================================================
    // Track Points
    // =========================================================================

    /**
     * All track points from the GPX file.
     */
    private final List<TrackPoint> trackPoints;

    /**
     * Simplified track points for map display.
     * Reduced using Douglas-Peucker algorithm.
     */
    private final List<TrackPoint> simplifiedTrack;

    /**
     * Total number of original track points.
     */
    private final int totalTrackPoints;

    // =========================================================================
    // Distance & Time Metrics
    // =========================================================================

    /**
     * Total distance in meters.
     */
    private final double distanceMeters;

    /**
     * Total duration in seconds.
     */
    private final long durationSeconds;

    /**
     * Average speed in meters per second.
     */
    private final double avgSpeedMps;

    /**
     * Maximum speed in meters per second.
     */
    private final double maxSpeedMps;

    // =========================================================================
    // Elevation Metrics
    // =========================================================================

    /**
     * Minimum elevation in meters.
     */
    private final Double elevationMin;

    /**
     * Maximum elevation in meters.
     */
    private final Double elevationMax;

    /**
     * Total elevation gain (D+) in meters.
     */
    private final Double elevationGain;

    /**
     * Total elevation loss (D-) in meters.
     */
    private final Double elevationLoss;

    // =========================================================================
    // Bounding Box
    // =========================================================================

    /**
     * Minimum latitude.
     */
    private final double boundsMinLat;

    /**
     * Maximum latitude.
     */
    private final double boundsMaxLat;

    /**
     * Minimum longitude.
     */
    private final double boundsMinLon;

    /**
     * Maximum longitude.
     */
    private final double boundsMaxLon;

    // =========================================================================
    // Convenience Methods
    // =========================================================================

    /**
     * Gets distance in kilometers.
     */
    public double getDistanceKm() {
        return distanceMeters / 1000.0;
    }

    /**
     * Gets average speed in km/h.
     */
    public double getAvgSpeedKmh() {
        return avgSpeedMps * 3.6;
    }

    /**
     * Gets the center of the bounding box.
     */
    public double[] getBoundsCenter() {
        return new double[] {
                (boundsMinLat + boundsMaxLat) / 2,
                (boundsMinLon + boundsMaxLon) / 2
        };
    }

    /**
     * Checks if elevation data is available.
     */
    public boolean hasElevationData() {
        return elevationMin != null && elevationMax != null;
    }
}