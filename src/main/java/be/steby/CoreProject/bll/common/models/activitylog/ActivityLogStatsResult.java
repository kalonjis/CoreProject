package be.steby.CoreProject.bll.common.models.activitylog;

import java.util.Map;

/**
 * Internal BLL result model carrying activity log statistics.
 *
 * <p>Produced by
 * {@link be.steby.CoreProject.bll.common.services.activitylog.ActivityLogQueryService#getStats}
 * and consumed by the PL layer, which maps it to
 * {@code ActivityLogStatsResponse} for serialisation. The BLL never knows
 * about the PL response type.</p>
 *
 * @param countsByCategory map of action category → log count,
 *                         e.g. {@code {"AUTH": 142, "SECURITY": 17}}
 * @param total            grand total across all categories
 */
public record ActivityLogStatsResult(
        Map<String, Long> countsByCategory,
        long total
) {}