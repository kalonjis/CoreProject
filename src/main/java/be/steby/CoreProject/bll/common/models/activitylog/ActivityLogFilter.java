package be.steby.CoreProject.bll.common.models.activitylog;

import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;

/**
 * Internal BLL filter model for activity log queries.
 *
 * <p>Carries all optional filter parameters needed by
 * {@link be.steby.CoreProject.bll.common.services.activitylog.ActivityLogQueryService}.
 * Built from the PL request DTO via
 * {@code ActivityLogFilterRequest.toActivityLogFilter()} — the BLL never
 * depends on PL types.</p>
 *
 * <p>All fields are nullable — a null value means "no filter applied"
 * for that dimension. {@link be.steby.CoreProject.dal.specifications.ActivityLogSpecification}
 * handles null-safety and simply skips the predicate.</p>
 *
 * @param user       target user; null for global queries (all users)
 * @param category   action category, e.g. {@code "AUTH"}, {@code "SECURITY"}; null for all
 * @param from       lower timestamp bound (inclusive); null for no lower bound
 * @param to         upper timestamp bound (inclusive); null for no upper bound
 * @param successful success status filter; null for both outcomes
 */
public record ActivityLogFilter(
        User user,
        String category,
        Instant from,
        Instant to,
        Boolean successful
) {

    /**
     * Returns a filter scoped to a specific user with no other constraints.
     * Convenience factory for user-history queries.
     *
     * @param user the target user
     * @return filter with only the user predicate set
     */
    public static ActivityLogFilter forUser(User user) {
        return new ActivityLogFilter(user, null, null, null, null);
    }

    /**
     * Returns a global filter with no constraints — matches all logs.
     *
     * @return empty filter (all logs)
     */
    public static ActivityLogFilter global() {
        return new ActivityLogFilter(null, null, null, null, null);
    }

    /**
     * Returns a copy of this filter with the given user injected.
     *
     * <p>Used by the controller after resolving the target user from the
     * path variable — {@code ActivityLogFilterRequest.toActivityLogFilter()}
     * intentionally leaves user null; the controller injects it here.</p>
     *
     * @param resolvedUser the resolved target user
     * @return new filter instance with user populated, all other fields unchanged
     */
    public ActivityLogFilter withUser(User resolvedUser) {
        return new ActivityLogFilter(resolvedUser, category(), from(), to(), successful());
    }
}