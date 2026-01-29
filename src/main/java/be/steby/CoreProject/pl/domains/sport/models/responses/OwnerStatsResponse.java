package be.steby.CoreProject.pl.domains.sport.models.responses;

import be.steby.CoreProject.bll.domains.sport.services.SportTrackService.OwnerStats;

/**
 * DTO for owner statistics.
 */
public record OwnerStatsResponse(
        long totalTracks,
        double totalDistanceKm,
        long totalDurationSeconds,
        double totalDurationHours,
        String totalDurationFormatted,
        double totalElevationGainM
) {
    /**
     * Creates DTO from service record.
     */
    public static OwnerStatsResponse fromStats(OwnerStats stats) {
        return new OwnerStatsResponse(
                stats.totalTracks(),
                Math.round(stats.totalDistanceKm() * 100.0) / 100.0,
                stats.totalDurationSeconds(),
                Math.round(stats.getTotalDurationHours() * 100.0) / 100.0,
                formatDuration(stats.totalDurationSeconds()),
                Math.round(stats.totalElevationGainM() * 10.0) / 10.0
        );
    }

    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return String.format("%dh %02dmin", hours, minutes);
    }
}