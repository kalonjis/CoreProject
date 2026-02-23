package be.steby.CoreProject.pl.domains.activitylog.models.requests;

import be.steby.CoreProject.bll.common.models.activitylog.ActivityLogFilter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;

/**
 * PL request model for activity log filter parameters.
 *
 * <p>Received as query parameters on admin endpoints. Converted to the
 * BLL-internal {@link ActivityLogFilter} via {@link #toActivityLogFilter()}
 * before being passed to the service layer — the BLL never sees PL types.</p>
 *
 * <p>All fields are optional. A missing field means "no filter" for that
 * dimension.</p>
 *
 * @param category   action category filter, e.g. {@code "AUTH"}, {@code "SECURITY"}
 * @param from       start date (inclusive), ISO format {@code yyyy-MM-dd}
 * @param to         end date (inclusive), ISO format {@code yyyy-MM-dd}
 * @param successful success status filter; null means both outcomes
 */
public record ActivityLogFilterRequest(
        String category,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,

        Boolean successful
) {

    /**
     * Converts this PL request to the BLL-internal {@link ActivityLogFilter}.
     *
     * <p>Date conversion: {@code LocalDate} bounds are expanded to UTC
     * day boundaries — {@code from} becomes start-of-day, {@code to}
     * becomes start-of-next-day (exclusive upper bound).</p>
     *
     * <p>The {@code user} field is intentionally left null here — caller
     * is responsible for resolving the target user from the path variable
     * and passing it separately to the service.</p>
     *
     * @return BLL filter with no user constraint; user must be set by the controller
     */
    public ActivityLogFilter toActivityLogFilter() {
        Instant fromInstant = from != null
                ? from.atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;

        Instant toInstant = to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;

        return new ActivityLogFilter(
                null,   // user resolved by the controller from path variable
                category != null ? category.toUpperCase() : null,
                fromInstant,
                toInstant,
                successful
        );
    }
}