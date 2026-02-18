package be.steby.CoreProject.pl.domains.activitylog.controllers;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogQueryService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.activitylog.models.responses.ActivityLogResponse;
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
 * <p>Two sets of endpoints are provided:</p>
 * <ul>
 *   <li><b>User-facing</b> ({@code /api/activity-logs/me/**}) — authenticated user
 *       queries their own logs only. No admin privilege required.</li>
 *   <li><b>Admin</b> ({@code /api/activity-logs/admin/**}) — admin queries any
 *       user's logs by public user ID.</li>
 * </ul>
 *
 * <p>All read operations. No write endpoints here — persistence is handled
 * asynchronously via the event/listener pipeline.</p>
 */
@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogController {

    private final ActivityLogQueryService activityLogQueryService;
    private final UserService userService;

    // =========================================================================
    // User-facing endpoints  — /me/**
    // =========================================================================

    /**
     * Returns the full paginated activity history of the authenticated user.
     *
     * <p>No category filter — covers AUTH, SECURITY and any future categories.
     * Useful for a "My activity" overview page.</p>
     *
     * <p>{@code GET /api/activity-logs/me?page=0&size=20}</p>
     *
     * @param user     the authenticated user (injected by Spring Security)
     * @param pageable page/sort parameters; default: 20 per page, newest first
     * @return paginated list of {@link ActivityLogResponse}
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ActivityLogResponse>> getMyHistory(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /me — user: {}", user.getUsername());

        Page<ActivityLogResponse> page = activityLogQueryService
                .getMyHistory(user, pageable)
                .map(ActivityLogResponse::from);

        return ResponseEntity.ok(page);
    }

    /**
     * Returns the AUTH-category log history of the authenticated user,
     * optionally filtered by a date range.
     *
     * <p>Covers: {@code LOGIN}, {@code LOGIN_FAILED}, {@code LOGOUT},
     * {@code TWO_FACTOR_FAILED}, {@code ACCOUNT_LOCKED}, {@code LOGIN_BLOCKED},
     * {@code FORCE_LOGOUT}.</p>
     *
     * <p>{@code GET /api/activity-logs/me/auth?from=2025-01-01&to=2025-02-01}</p>
     *
     * @param user     the authenticated user
     * @param from     start date (inclusive), ISO date format {@code yyyy-MM-dd}; optional
     * @param to       end date (inclusive), ISO date format; optional
     * @param pageable page/sort parameters
     * @return paginated list of AUTH logs
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

        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant   = to   != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Page<ActivityLogResponse> page = activityLogQueryService
                .getMyAuthHistory(user, fromInstant, toInstant, pageable)
                .map(ActivityLogResponse::from);

        return ResponseEntity.ok(page);
    }

    /**
     * Returns the SECURITY-category log history of the authenticated user,
     * optionally filtered by a date range.
     *
     * <p>Covers entries produced by {@code SecurityAction}: brute-force detections,
     * IP/username blocks, high-risk alerts, etc.</p>
     *
     * <p>{@code GET /api/activity-logs/me/security?from=2025-01-01}</p>
     *
     * @param user     the authenticated user
     * @param from     start date (inclusive), optional
     * @param to       end date (inclusive), optional
     * @param pageable page/sort parameters
     * @return paginated list of SECURITY logs
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

        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant   = to   != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Page<ActivityLogResponse> page = activityLogQueryService
                .getMySecurityHistory(user, fromInstant, toInstant, pageable)
                .map(ActivityLogResponse::from);

        return ResponseEntity.ok(page);
    }

    // =========================================================================
    // Admin endpoints — /admin/**
    // =========================================================================

    /**
     * Returns the full paginated activity history of any user, identified by
     * their public ID.
     *
     * <p>{@code GET /api/activity-logs/admin/users/{publicUserId}}</p>
     *
     * @param publicUserId public UUID of the target user
     * @param pageable     page/sort parameters
     * @return paginated list of logs
     */
    @GetMapping("/admin/users/{publicUserId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ActivityLogResponse>> getUserHistory(
            @PathVariable String publicUserId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /admin/users/{} — full history", publicUserId);

        User target = userService.getUserByPublicId(publicUserId);

        Page<ActivityLogResponse> page = activityLogQueryService
                .getUserHistory(target, pageable)
                .map(ActivityLogResponse::from);

        return ResponseEntity.ok(page);
    }

    /**
     * Returns a category-filtered, date-range-filtered paginated view of logs
     * for any user. Admin use only.
     *
     * <p>{@code GET /api/activity-logs/admin/users/{publicUserId}/category/AUTH?from=...&to=...}</p>
     *
     * @param publicUserId public UUID of the target user
     * @param category     action category, case-insensitive (e.g. {@code AUTH}, {@code SECURITY})
     * @param from         start date (inclusive), optional
     * @param to           end date (inclusive), optional
     * @param pageable     page/sort parameters
     * @return paginated list of logs for the given category
     */
    @GetMapping("/admin/users/{publicUserId}/category/{category}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ActivityLogResponse>> getUserHistoryByCategory(
            @PathVariable String publicUserId,
            @PathVariable String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("GET /admin/users/{}/category/{}", publicUserId, category);

        User target = userService.getUserByPublicId(publicUserId);

        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant   = to   != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Page<ActivityLogResponse> page = activityLogQueryService
                .getUserHistoryByCategory(target, category, fromInstant, toInstant, pageable)
                .map(ActivityLogResponse::from);

        return ResponseEntity.ok(page);
    }
}