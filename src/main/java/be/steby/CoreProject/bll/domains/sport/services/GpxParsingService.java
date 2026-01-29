package be.steby.CoreProject.bll.domains.sport.services;

import be.steby.CoreProject.bll.domains.sport.models.GpxParseResult;
import be.steby.CoreProject.bll.domains.sport.models.TrackPoint;

import java.io.InputStream;
import java.util.List;

/**
 * Service for parsing GPX files and extracting track data.
 *
 * <p>Handles the full parsing of GPX files including:
 * <ul>
 *   <li>Extraction of all track points (lat, lon, ele, time)</li>
 *   <li>Calculation of metrics (distance, duration, elevation, speed)</li>
 *   <li>Track simplification for map display</li>
 *   <li>Bounding box calculation</li>
 * </ul>
 *
 * <p>This service focuses on parsing and calculations only.
 * Persistence is handled by {@link SportTrackService}.
 *
 * @see GpxParseResult
 * @see SportTrackService
 */
public interface GpxParsingService {

    /**
     * Parses a GPX file and extracts all data and metrics.
     *
     * @param inputStream the GPX file input stream
     * @param filename    original filename (for metadata extraction)
     * @return complete parse result with points, metrics, and simplified track
     * @throws GpxParsingException if parsing fails
     */
    GpxParseResult parse(InputStream inputStream, String filename);

    /**
     * Parses a GPX file from storage by its public ID.
     *
     * @param gpxFilePublicId public ID of the stored GPX file
     * @return complete parse result
     * @throws GpxParsingException      if parsing fails
     * @throws FileNotFoundException    if file not found
     */
    GpxParseResult parseFromStorage(String gpxFilePublicId);

    /**
     * Simplifies a track using Douglas-Peucker algorithm.
     * Reduces points while preserving the track shape.
     *
     * @param points    the original track points
     * @param maxPoints maximum number of points in result
     * @return simplified list of points
     */
    List<TrackPoint> simplifyTrack(List<TrackPoint> points, int maxPoints);

    /**
     * Calculates total distance between consecutive points.
     * Uses Haversine formula for accurate geodesic distance.
     *
     * @param points the track points
     * @return total distance in meters
     */
    double calculateDistance(List<TrackPoint> points);

    /**
     * Calculates elevation gain (D+) with smoothing.
     * Applies threshold to filter GPS noise.
     *
     * @param points    the track points with elevation data
     * @param threshold minimum elevation change to count (meters)
     * @return total elevation gain in meters
     */
    double calculateElevationGain(List<TrackPoint> points, double threshold);

    /**
     * Calculates elevation loss (D-) with smoothing.
     *
     * @param points    the track points with elevation data
     * @param threshold minimum elevation change to count (meters)
     * @return total elevation loss in meters (positive value)
     */
    double calculateElevationLoss(List<TrackPoint> points, double threshold);
}