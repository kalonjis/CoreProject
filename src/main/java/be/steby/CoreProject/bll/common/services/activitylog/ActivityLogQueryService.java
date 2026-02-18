package be.steby.CoreProject.bll.common.services.activitylog;

import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
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

/**
 * Read-only service for querying activity logs.
 *
 * <p>Intentionally separate from {@link ActivityLogService} (write side) to
 * respect CQRS: writes are async / fire-and-forget; reads are synchronous,
 * transactional and called from controller layer.</p>
 *
 * <p>All public methods are {@code readOnly = true} transactions — no
 * accidental writes possible here.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ActivityLogQueryService {

    /** Default look-back window when no explicit date range is supplied. */
    private static final int DEFAULT_DAYS = 30;

    private final ActivityLogRepository activityLogRepository;

    // =========================================================================
    // User-facing queries (own logs only)
    // =========================================================================

    /**
     * Returns a paginated view of all activity logs for the given user,
     * newest first.
     *
     * @param user     the authenticated user requesting their own history
     * @param pageable page / sort parameters (default: 20 per page, desc)
     * @return page of logs
     */
    public Page<ActivityLog> getMyHistory(User user, Pageable pageable) {
        log.debug("Fetching full history for user: {}", user.getUsername());
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    /**
     * Returns a paginated view of auth-category logs for the given user,
     * optionally filtered by a time window.
     *
     * <p>When {@code from} / {@code to} are null, defaults to the last
     * {@value #DEFAULT_DAYS} days.</p>
     *
     * @param user     the authenticated user
     * @param from     start of the time window (inclusive), nullable
     * @param to       end of the time window (inclusive), nullable
     * @param pageable page / sort parameters
     * @return page of AUTH-category logs
     */
    public Page<ActivityLog> getMyAuthHistory(User user, Instant from, Instant to, Pageable pageable) {
        Instant resolvedFrom = from != null ? from : Instant.now().minus(DEFAULT_DAYS, ChronoUnit.DAYS);
        Instant resolvedTo   = to   != null ? to   : Instant.now();

        log.debug("Fetching AUTH history for user: {} — {} → {}",
                user.getUsername(), resolvedFrom, resolvedTo);

        return activityLogRepository.findByUserAndActionCategoryAndTimestampBetween(
                user, "AUTH", resolvedFrom, resolvedTo, pageable);
    }

    /**
     * Returns a paginated view of security-category logs for the given user,
     * optionally filtered by a time window.
     *
     * <p>Covers {@code ACCOUNT_LOCKED}, {@code LOGIN_BLOCKED} and all
     * {@code SecurityAction} entries linked to the user.</p>
     *
     * @param user     the authenticated user
     * @param from     start of the time window (inclusive), nullable
     * @param to       end of the time window (inclusive), nullable
     * @param pageable page / sort parameters
     * @return page of SECURITY-category logs
     */
    public Page<ActivityLog> getMySecurityHistory(User user, Instant from, Instant to, Pageable pageable) {
        Instant resolvedFrom = from != null ? from : Instant.now().minus(DEFAULT_DAYS, ChronoUnit.DAYS);
        Instant resolvedTo   = to   != null ? to   : Instant.now();

        log.debug("Fetching SECURITY history for user: {} — {} → {}",
                user.getUsername(), resolvedFrom, resolvedTo);

        return activityLogRepository.findByUserAndActionCategoryAndTimestampBetween(
                user, "SECURITY", resolvedFrom, resolvedTo, pageable);
    }

    // =========================================================================
    // Admin queries
    // =========================================================================

    /**
     * Returns a paginated view of all activity logs for any given user.
     * Admin use only — no category filter.
     *
     * @param user     the target user
     * @param pageable page / sort parameters
     * @return page of logs
     */
    public Page<ActivityLog> getUserHistory(User user, Pageable pageable) {
        log.debug("Admin: fetching full history for user: {}", user.getUsername());
        return activityLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    /**
     * Returns a paginated, category-filtered view of logs for any user.
     * Admin use only.
     *
     * @param user     the target user
     * @param category action category string, e.g. {@code "AUTH"}, {@code "SECURITY"}
     * @param from     start of the time window (inclusive), nullable
     * @param to       end of the time window (inclusive), nullable
     * @param pageable page / sort parameters
     * @return page of logs
     */
    public Page<ActivityLog> getUserHistoryByCategory(User user, String category,
                                                       Instant from, Instant to,
                                                       Pageable pageable) {
        Instant resolvedFrom = from != null ? from : Instant.now().minus(DEFAULT_DAYS, ChronoUnit.DAYS);
        Instant resolvedTo   = to   != null ? to   : Instant.now();

        log.debug("Admin: fetching {} history for user: {} — {} → {}",
                category, user.getUsername(), resolvedFrom, resolvedTo);

        return activityLogRepository.findByUserAndCategoryBetweenForAdmin(
                user, category.toUpperCase(), resolvedFrom, resolvedTo, pageable);
    }
}