package be.steby.CoreProject.pl.domains.activitylog.models.responses;

import be.steby.CoreProject.bll.common.models.activitylog.ActivityLogStatsResult;

import java.util.Map;

/**
 * PL response DTO for activity log statistics.
 *
 * <p>Serialised as JSON and returned by
 * {@code GET /api/activity-logs/admin/stats}. Built from the BLL-internal
 * {@link ActivityLogStatsResult} via {@link #from(ActivityLogStatsResult)} —
 * the BLL never depends on this type.</p>
 *
 * @param countsByCategory map of action category → log count,
 *                         e.g. {@code {"AUTH": 142, "SECURITY": 17}}
 * @param total            grand total across all categories
 */
public record ActivityLogStatsResponse(
        Map<String, Long> countsByCategory,
        long total
) {

    /**
     * Maps a BLL {@link ActivityLogStatsResult} to this PL response record.
     *
     * @param result the BLL result; must not be null
     * @return the corresponding response record
     */
    public static ActivityLogStatsResponse from(ActivityLogStatsResult result) {
        return new ActivityLogStatsResponse(
                result.countsByCategory(),
                result.total()
        );
    }
}