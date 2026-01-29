package be.steby.CoreProject.bll.domains.sport.models;

import java.time.Instant;

/**
 * Represents a single point in a GPS track.
 *
 * <p>Immutable record containing coordinates, elevation, and timestamp
 * extracted from a GPX trackpoint element.
 *
 * @param latitude   latitude in decimal degrees (-90 to 90)
 * @param longitude  longitude in decimal degrees (-180 to 180)
 * @param elevation  elevation in meters (nullable if not present in GPX)
 * @param timestamp  point timestamp (nullable if not present in GPX)
 */
public record TrackPoint(
        double latitude,
        double longitude,
        Double elevation,
        Instant timestamp
) {

    /**
     * Creates a TrackPoint with only coordinates (no elevation/time).
     */
    public static TrackPoint of(double latitude, double longitude) {
        return new TrackPoint(latitude, longitude, null, null);
    }

    /**
     * Creates a TrackPoint with coordinates and elevation.
     */
    public static TrackPoint of(double latitude, double longitude, double elevation) {
        return new TrackPoint(latitude, longitude, elevation, null);
    }

    /**
     * Checks if this point has elevation data.
     */
    public boolean hasElevation() {
        return elevation != null;
    }

    /**
     * Checks if this point has timestamp data.
     */
    public boolean hasTimestamp() {
        return timestamp != null;
    }

    /**
     * Converts to JSON array format for frontend: [lat, lon, ele]
     * Elevation is 0 if not present.
     */
    public double[] toArray() {
        return new double[] {
                latitude,
                longitude,
                elevation != null ? elevation : 0.0
        };
    }
}