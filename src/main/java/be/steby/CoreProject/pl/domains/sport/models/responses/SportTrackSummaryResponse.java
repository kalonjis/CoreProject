package be.steby.CoreProject.pl.domains.sport.models.responses;

import be.steby.CoreProject.dl.entities.SportTrack;
import be.steby.CoreProject.dl.enums.SportType;

import java.time.Instant;

/**
 * Summary DTO for sport track list views.
 *
 * <p>Lightweight version without track data (no simplifiedTrack, no elevationProfile).
 * Used for paginated lists to reduce payload size.
 */
public record SportTrackSummaryResponse(
        String publicId,
        String name,
        SportType sportType,
        Instant startedAt,
        double distanceKm,
        long durationSeconds,
        String durationFormatted,
        double avgSpeedKmh,
        Double elevationGain,
        Instant createdAt
) {
    /**
     * Creates summary DTO from entity.
     */
    public static SportTrackSummaryResponse fromEntity(SportTrack track) {
        return new SportTrackSummaryResponse(
                track.getPublicId(),
                track.getName(),
                track.getSportType(),
                track.getStartedAt(),
                track.getDistanceKm(),
                track.getDurationSeconds(),
                formatDuration(track.getDurationSeconds()),
                track.getAvgSpeedKmh(),
                track.getElevationGain(),
                track.getCreatedAt()
        );
    }

    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }
}