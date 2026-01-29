package be.steby.CoreProject.pl.domains.sport.models.responses;

import be.steby.CoreProject.dl.entities.SportTrack;
import be.steby.CoreProject.dl.enums.SportType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;


/**
 * Full DTO for sport track details.
 *
 * <p>Includes all metrics, track data for map display, and elevation profile.
 * Used for single track detail views.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SportTrackResponse(
        // Identity
        String publicId,
        String gpxFilePublicId,

        // Metadata
        String name,
        SportType sportType,
        Instant startedAt,
        Instant endedAt,

        // Distance & Time
        double distanceMeters,
        double distanceKm,
        long durationSeconds,
        String durationFormatted,
        double avgSpeedMps,
        double avgSpeedKmh,
        Double maxSpeedMps,
        Double maxSpeedKmh,

        // Elevation
        Double elevationMin,
        Double elevationMax,
        Double elevationGain,
        Double elevationLoss,

        // Track data (JSON strings for frontend parsing)
        int totalTrackPoints,
        double[][] simplifiedTrack,
        double[][] elevationProfile,

        // Bounding box
        Double boundsMinLat,
        Double boundsMaxLat,
        Double boundsMinLon,
        Double boundsMaxLon,
        double[] boundsCenter,

        // Audit
        Instant createdAt
) {
    /**
     * Creates DTO from entity.
     */
    public static SportTrackResponse fromEntity(SportTrack track, ObjectMapper objectMapper) {
        return new SportTrackResponse(
                track.getPublicId(),
                track.getGpxFilePublicId(),
                track.getName(),
                track.getSportType(),
                track.getStartedAt(),
                track.getEndedAt(),
                track.getDistanceMeters(),
                track.getDistanceKm(),
                track.getDurationSeconds(),
                formatDuration(track.getDurationSeconds()),
                track.getAvgSpeedMps(),
                track.getAvgSpeedKmh(),
                track.getMaxSpeedMps(),
                track.getMaxSpeedKmh(),
                track.getElevationMin(),
                track.getElevationMax(),
                track.getElevationGain(),
                track.getElevationLoss(),
                track.getTotalTrackPoints(),
                parseJson(track.getSimplifiedTrack(), objectMapper),
                parseJson(track.getElevationProfile(), objectMapper),
                track.getBoundsMinLat(),
                track.getBoundsMaxLat(),
                track.getBoundsMinLon(),
                track.getBoundsMaxLon(),
                track.getBoundsCenter(),
                track.getCreatedAt()
        );
    }

    /**
     * Formats duration as HH:mm:ss or mm:ss.
     */
    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Parses JSON string to double[][] array.
     *
     * @param json         the JSON string
     * @param objectMapper Jackson ObjectMapper
     * @return parsed array or empty array on error
     */
    private static double[][] parseJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return new double[0][];
        }
        try {
            return objectMapper.readValue(json, double[][].class);
        } catch (Exception e) {
            return new double[0][];
        }
    }
}