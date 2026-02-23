package be.steby.CoreProject.pl.domains.activitylog.controllers;

import be.steby.CoreProject.bll.common.models.activitylog.ActivityLogFilter;
import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogQueryService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.activitylog.models.requests.ActivityLogFilterRequest;
import be.steby.CoreProject.pl.domains.activitylog.models.responses.ActivityLogResponse;
import be.steby.CoreProject.pl.domains.activitylog.models.responses.ActivityLogStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * REST controller for activity log queries.
 *
 * <p>Two families of endpoints:</p>
 * <ul>
 *   <li><b>User-facing</b> {@code /api/activity-logs/me/**} — authenticated user
 *       queries their own logs only; no admin privilege required.</li>
 *   <li><b>Admin</b> {@code /api/activity-logs/admin/**} — admin queries any
 *       user's logs or the full global history.</li>
 * </ul>
 *
 * <p>All read operations. Writes are handled asynchronously via the
 * event/listener pipeline and never touch this controller.</p>
 *
 * <p>The PL→BLL boundary is respected throughout: {@link ActivityLogFilterRequest}
 * is converted to {@link ActivityLogFilter} before reaching the service layer.</p>
 */
@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogController {

    private final ActivityLogQueryService activityLogQueryService;
    private final UserService userService;

    // =========================================================================
    // User-facing endpoints — /me/**
    // =========================================================================

    /**
     * Full activity history of the authenticated user, newest first.
     * No category filter — covers every action category.
     *
     * <p>{@code GET /api/activity-logs/me}</p>
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ActivityLogResponse>> getMyHistory(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /me — user: {}", user.getUsername());

        return ResponseEntity.ok(
                activityLogQueryService.getMyHistory(user, pageable)
                        .map(ActivityLogResponse::from));
    }

    /**
     * AUTH-category logs of the authenticated user, optionally date-filtered.
     * Defaults to last 30 days when no range is provided.
     *
     * <p>{@code GET /api/activity-logs/me/auth?from=2025-01-01&to=2025-02-01}</p>
     */
    @GetMapping("/me/auth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ActivityLogResponse>> getMyAuthHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /me/auth — user: {}, from: {}, to: {}", user.getUsername(), from, to);

        return ResponseEntity.ok(
                activityLogQueryService.getMyAuthHistory(user, toFromInstant(from), toToInstant(to), pageable)
                        .map(ActivityLogResponse::from));
    }

    /**
     * SECURITY-category logs of the authenticated user, optionally date-filtered.
     * Defaults to last 30 days when no range is provided.
     *
     * <p>{@code GET /api/activity-logs/me/security?from=2025-01-01}</p>
     */
    @GetMapping("/me/security")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ActivityLogResponse>> getMySecurityHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /me/security — user: {}, from: {}, to: {}", user.getUsername(), from, to);

        return ResponseEntity.ok(
                activityLogQueryService.getMySecurityHistory(user, toFromInstant(from), toToInstant(to), pageable)
                        .map(ActivityLogResponse::from));
    }

    // =========================================================================
    // Admin endpoints — user-scoped /admin/users/{publicUserId}/**
    // =========================================================================

    /**
     * Activity logs for any user, with optional filters on category, date range
     * and success status. Admin only.
     *
     * <p>Filter params are optional and combinable:
     * {@code ?category=AUTH&from=2025-01-01&to=2025-02-01&successful=false}</p>
     *
     * <p>{@code GET /api/activity-logs/admin/users/{publicUserId}}</p>
     */
    @GetMapping("/admin/users/{publicUserId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ActivityLogResponse>> getUserLogs(
            @PathVariable String publicUserId,
            @ModelAttribute ActivityLogFilterRequest filterRequest,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /admin/users/{} — filter: {}", publicUserId, filterRequest);

        User target = userService.getUserByPublicId(publicUserId);

        // Convert PL request → BLL filter, then inject the resolved user
        ActivityLogFilter filter = filterRequest.toActivityLogFilter()
                .withUser(target);

        return ResponseEntity.ok(
                activityLogQueryService.getUserLogs(filter, pageable)
                        .map(ActivityLogResponse::from));
    }

    // =========================================================================
    // Admin endpoints — global /admin/all/**
    // =========================================================================

    /**
     * All logs across all users, with optional filters on category, date range
     * and success status. Admin only.
     *
     * <p>{@code GET /api/activity-logs/admin/all?category=SECURITY&successful=false}</p>
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ActivityLogResponse>> getAllLogs(
            @ModelAttribute ActivityLogFilterRequest filterRequest,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /admin/all — filter: {}", filterRequest);

        ActivityLogFilter filter = filterRequest.toActivityLogFilter();

        return ResponseEntity.ok(
                activityLogQueryService.getAllLogs(filter, pageable)
                        .map(ActivityLogResponse::from));
    }

    // =========================================================================
    // Admin endpoints — stats /admin/stats
    // =========================================================================

    /**
     * Log counts grouped by action category, across all users.
     * Optionally scoped to a date range; no range = entire history.
     *
     * <p>{@code GET /api/activity-logs/admin/stats?from=2025-01-01&to=2025-02-01}</p>
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ActivityLogStatsResponse> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.debug("GET /admin/stats — from: {}, to: {}", from, to);

        return ResponseEntity.ok(
                ActivityLogStatsResponse.from(
                        activityLogQueryService.getStats(toFromInstant(from), toToInstant(to))));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Converts a nullable {@link LocalDate} lower bound to a UTC {@link Instant}.
     * Resolves to start-of-day (inclusive).
     */
    private Instant toFromInstant(LocalDate date) {
        return date != null ? date.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
    }

    /**
     * Converts a nullable {@link LocalDate} upper bound to a UTC {@link Instant}.
     * Resolves to start-of-next-day so the given date is fully included (exclusive upper bound).
     * e.g. to=2026-02-23 → 2026-02-24T00:00:00Z — covers the entire day.
     */
    private Instant toToInstant(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
    }
}