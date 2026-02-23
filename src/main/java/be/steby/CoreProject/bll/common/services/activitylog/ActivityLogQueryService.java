package be.steby.CoreProject.bll.common.services.activitylog;

import be.steby.CoreProject.bll.common.models.activitylog.ActivityLogFilter;
import be.steby.CoreProject.bll.common.models.activitylog.ActivityLogStatsResult;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dal.specifications.ActivityLogSpecification;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only service for querying activity logs.
 *
 * <p>Intentionally separate from {@link ActivityLogService} (write side) to
 * respect CQRS: writes are async / fire-and-forget; reads are synchronous,
 * transactional and called from the controller layer.</p>
 *
 * <p>All filtering is delegated to {@link ActivityLogSpecification} via
 * {@link ActivityLogFilter} — a single {@code repository.findAll(spec, pageable)}
 * call covers every combination. No combinatorial explosion of repository methods.</p>
 *
 * <p>User-facing ({@code /me/**}) methods apply a default date window when no
 * range is supplied. Admin methods do not — admins may want full history.</p>
 *
 * <p>All public methods are {@code readOnly = true} — no accidental writes.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ActivityLogQueryService {

    /** Default look-back window for user-facing queries when no date range is supplied. */
    private static final int DEFAULT_DAYS = 30;

    private final ActivityLogRepository activityLogRepository;

    // =========================================================================
    // User-facing queries — /me/**
    // =========================================================================

    /**
     * Returns all activity logs for the authenticated user, newest first.
     * No category filter — covers every action category.
     *
     * @param user     the authenticated user
     * @param pageable page / sort parameters
     * @return page of logs
     */
    public Page<ActivityLog> getMyHistory(User user, Pageable pageable) {
        log.debug("Fetching full history for user: {}", user.getUsername());
        return activityLogRepository.findAll(
                ActivityLogSpecification.build(ActivityLogFilter.forUser(user)), pageable);
    }

    /**
     * Returns AUTH-category logs for the authenticated user, optionally
     * filtered by a time window. Defaults to last {@value #DEFAULT_DAYS} days.
     *
     * @param user     the authenticated user
     * @param from     start of the time window (inclusive), nullable
     * @param to       end of the time window (inclusive), nullable
     * @param pageable page / sort parameters
     * @return page of AUTH-category logs
     */
    public Page<ActivityLog> getMyAuthHistory(User user, Instant from, Instant to, Pageable pageable) {
        Instant resolvedFrom = resolveFrom(from);
        Instant resolvedTo   = resolveTo(to);

        log.debug("Fetching AUTH history for user: {} — {} → {}",
                user.getUsername(), resolvedFrom, resolvedTo);

        ActivityLogFilter filter = new ActivityLogFilter(user, "AUTH", resolvedFrom, resolvedTo, null);
        return activityLogRepository.findAll(ActivityLogSpecification.build(filter), pageable);
    }

    /**
     * Returns SECURITY-category logs for the authenticated user, optionally
     * filtered by a time window. Defaults to last {@value #DEFAULT_DAYS} days.
     *
     * @param user     the authenticated user
     * @param from     start of the time window (inclusive), nullable
     * @param to       end of the time window (inclusive), nullable
     * @param pageable page / sort parameters
     * @return page of SECURITY-category logs
     */
    public Page<ActivityLog> getMySecurityHistory(User user, Instant from, Instant to, Pageable pageable) {
        Instant resolvedFrom = resolveFrom(from);
        Instant resolvedTo   = resolveTo(to);

        log.debug("Fetching SECURITY history for user: {} — {} → {}",
                user.getUsername(), resolvedFrom, resolvedTo);

        ActivityLogFilter filter = new ActivityLogFilter(user, "SECURITY", resolvedFrom, resolvedTo, null);
        return activityLogRepository.findAll(ActivityLogSpecification.build(filter), pageable);
    }

    // =========================================================================
    // Admin queries — user-scoped
    // =========================================================================

    /**
     * Returns logs for a specific user matching the given filter.
     * Admin use only.
     *
     * <p>The {@code filter.user()} must be non-null — set by the controller
     * after resolving the target user from the path variable. No default date
     * window is applied; admin may query full history.</p>
     *
     * @param filter   BLL filter built from {@code ActivityLogFilterRequest.toActivityLogFilter()}
     *                 with {@code user} populated by the controller
     * @param pageable page / sort parameters
     * @return page of logs matching the filter
     */
    public Page<ActivityLog> getUserLogs(ActivityLogFilter filter, Pageable pageable) {
        log.debug("Admin: fetching logs for user: {} — category={}, from={}, to={}, successful={}",
                filter.user() != null ? filter.user().getUsername() : "null",
                filter.category(), filter.from(), filter.to(), filter.successful());

        return activityLogRepository.findAll(ActivityLogSpecification.build(filter), pageable);
    }

    // =========================================================================
    // Admin queries — global (all users)
    // =========================================================================

    /**
     * Returns logs across all users matching the given filter.
     * Admin use only.
     *
     * <p>{@code filter.user()} is expected to be null here — the controller
     * passes a filter built without a user constraint. No default date window
     * applied; admin may query the full history.</p>
     *
     * @param filter   BLL filter; user must be null for global queries
     * @param pageable page / sort parameters
     * @return page of logs matching the filter
     */
    public Page<ActivityLog> getAllLogs(ActivityLogFilter filter, Pageable pageable) {
        log.debug("Admin: fetching global logs — category={}, from={}, to={}, successful={}",
                filter.category(), filter.from(), filter.to(), filter.successful());

        return activityLogRepository.findAll(ActivityLogSpecification.build(filter), pageable);
    }

    // =========================================================================
    // Admin queries — stats
    // =========================================================================

    /**
     * Returns a summary of log counts grouped by action category, across all users.
     *
     * <p>When {@code from} / {@code to} are null, counts cover the entire history
     * with no date restriction.</p>
     *
     * @param from lower timestamp bound, nullable
     * @param to   upper timestamp bound, nullable
     * @return {@link ActivityLogStatsResult} with per-category counts and grand total
     */
    public ActivityLogStatsResult getStats(Instant from, Instant to) {
        log.debug("Admin: computing global stats — from={}, to={}", from, to);

        List<Object[]> rows = (from != null || to != null)
                ? activityLogRepository.countByActionCategoryBetween(
                from != null ? from : Instant.EPOCH,
                to   != null ? to   : Instant.now())
                : activityLogRepository.countByActionCategory();

        Map<String, Long> countsByCategory = rows.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long)   row[1]));

        long total = countsByCategory.values().stream().mapToLong(Long::longValue).sum();

        return new ActivityLogStatsResult(countsByCategory, total);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Instant resolveFrom(Instant from) {
        return from != null ? from : Instant.now().minus(DEFAULT_DAYS, ChronoUnit.DAYS);
    }

    private Instant resolveTo(Instant to) {
        return to != null ? to : Instant.now();
    }
}