package be.steby.CoreProject.il.routes.sport;

/**
 * Security routes configuration for Sport domain.
 *
 * <p>Defines access rules for sport track endpoints.
 *
 * <p>Route categories:
 * <ul>
 *   <li>PUBLIC: None - all endpoints require authentication</li>
 *   <li>AUTHENTICATED: All sport track operations</li>
 * </ul>
 *
 * @see be.steby.CoreProject.il.routes.SecurityRoutesAggregator
 * @see be.steby.CoreProject.pl.domains.sport.controllers.SportTrackController
 */
public final class SportRoutes {

    // ========== PUBLIC ROUTES ==========

    /**
     * Public sport routes.
     * Currently none - all operations require authentication.
     */
    public static final String[] PUBLIC = {
            // No public routes
    };

    // ========== AUTHENTICATED ROUTES ==========

    /**
     * Routes requiring authentication (any logged-in user).
     *
     * <p>Includes:
     * <ul>
     *   <li>GPX import</li>
     *   <li>List sport tracks</li>
     *   <li>View sport track details</li>
     *   <li>Update/delete sport tracks</li>
     *   <li>Get statistics</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
            "/api/sport-tracks",          // GET: List, POST: Import
            "/api/sport-tracks/import",   // POST: Import GPX
            "/api/sport-tracks/stats",    // GET: Statistics
            "/api/sport-tracks/*"         // GET, PATCH, DELETE: Single track
    };

    // ========== ADMIN ROUTES ==========

    /**
     * Routes requiring ADMIN or SUPER_ADMIN role.
     * Future: Admin sport management capabilities.
     */
    public static final String[] ADMIN = {
            // Future: Admin endpoints
    };


    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * <p>Import endpoint uses JSON body, but may need CSRF bypass
     * depending on frontend configuration.</p>
     */
    public static final String[] CSRF_IGNORE = {
            "/api/sport-tracks/import",   // POST: Import GPX
            "/api/sport-tracks/*"         // PATCH, DELETE
    };

    private SportRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}