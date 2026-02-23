package be.steby.CoreProject.il.routes.activitylog;

/**
 * Security routes configuration for the ActivityLog domain.
 *
 * <p>Two access levels:</p>
 * <ul>
 *   <li><b>AUTHENTICATED</b> — {@code /me/**} — any logged-in user can query
 *       their own logs.</li>
 *   <li><b>ADMIN</b> — {@code /admin/**} — requires {@code ADMIN} or
 *       {@code SUPER_ADMIN} authority; handled by {@code SecurityConfig}
 *       via {@code SecurityRoutesAggregator.ADMIN_ROUTES}.</li>
 * </ul>
 */
public final class ActivityLogRoutes {

    // =========================================================================
    // Public — no authentication required
    // =========================================================================

    public static final String[] PUBLIC = {
            // No public activity log routes
    };

    // =========================================================================
    // Authenticated — any logged-in user (own logs only)
    // =========================================================================

    public static final String[] AUTHENTICATED = {
            "/api/activity-logs/me",
            "/api/activity-logs/me/**"
    };

    // =========================================================================
    // Admin — ADMIN | SUPER_ADMIN only
    // =========================================================================

    public static final String[] ADMIN = {
            "/api/activity-logs/admin/**"
    };

    // =========================================================================
    // CSRF
    // =========================================================================

    /**
     * Activity log endpoints use httpOnly cookies — CSRF protection must be kept.
     * No routes exempted.
     */
    public static final String[] CSRF_IGNORE = {
            // Empty — all endpoints are cookie-based and require CSRF protection
    };

    private ActivityLogRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}