package be.steby.CoreProject.il.routes.calendar;

/**
 * Security routes configuration for Calendar domain.
 * 
 * <p>Defines access rules for calendar event endpoints.</p>
 * 
 * <p>Route categories:</p>
 * <ul>
 *   <li>PUBLIC: None - all calendar operations require authentication</li>
 *   <li>AUTHENTICATED: All event CRUD operations and exports</li>
 *   <li>ADMIN: None - users manage their own events only</li>
 * </ul>
 * 
 * @see be.steby.CoreProject.il.routes.SecurityRoutesAggregator
 * @see be.steby.CoreProject.pl.domains.calendar.controller.CalendarController
 */
public final class CalendarRoutes {

    // ========== PUBLIC ROUTES ==========

    /**
     * Public calendar routes.
     * 
     * <p>Currently empty - all calendar operations require authentication.
     * Future: Public calendar sharing URLs could be added here.</p>
     */
    public static final String[] PUBLIC = {
            // No public routes
    };

    // ========== AUTHENTICATED ROUTES ==========

    /**
     * Routes requiring authentication (any logged-in user).
     * 
     * <p>Includes:</p>
     * <ul>
     *   <li>POST   /api/calendar/events → Create event</li>
     *   <li>GET    /api/calendar/events → List user's events</li>
     *   <li>GET    /api/calendar/events/range → Events in date range</li>
     *   <li>GET    /api/calendar/events/upcoming → Upcoming events</li>
     *   <li>GET    /api/calendar/events/{id} → Get event details</li>
     *   <li>PUT    /api/calendar/events/{id} → Update event</li>
     *   <li>POST   /api/calendar/events/{id}/cancel → Cancel event</li>
     *   <li>DELETE /api/calendar/events/{id} → Delete event</li>
     *   <li>GET    /api/calendar/events/{id}/export → Export .ics file</li>
     *   <li>GET    /api/calendar/events/{id}/urls → Get calendar URLs</li>
     *   <li>GET    /api/calendar/export-all → Export all events as .ics</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
            "/api/calendar/events",           // POST: Create, GET: List
            "/api/calendar/events/range",     // GET: Date range query
            "/api/calendar/events/status/*",     // GET
            "/api/calendar/events/upcoming",  // GET: Upcoming events
            "/api/calendar/events/*",         // GET, PUT, DELETE: Single event
            "/api/calendar/events/*/cancel",  // POST: Cancel event
            "/api/calendar/events/*/export",  // GET: Export .ics
            "/api/calendar/events/*/urls",    // GET: Calendar URLs
            "/api/calendar/export-all"        // GET: Export all events
    };

    // ========== ADMIN ROUTES ==========

    /**
     * Admin calendar routes.
     * 
     * <p>Currently empty - calendar is user-scoped only.
     * Future: Admin endpoints for viewing/managing all calendars could be added.</p>
     */
    public static final String[] ADMIN = {
            // Future: Admin endpoints for calendar management
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     * 
     * <p>Calendar operations use JSON payloads and are authenticated.
     * Export endpoints are GET requests (idempotent, no state change).</p>
     * 
     * <p>⚠️ TODO PRODUCTION: Review if CSRF protection is needed for
     * state-changing operations (POST, PUT, DELETE). Consider enabling
     * CSRF for better security if frontend can handle it.</p>
     */
    public static final String[] CSRF_IGNORE = {
            "/api/calendar/events",           // POST, GET
            "/api/calendar/events/range",     // GET
            "/api/calendar/events/status/*",     // GET
            "/api/calendar/events/upcoming",  // GET
            "/api/calendar/events/*",         // PUT, DELETE, GET
            "/api/calendar/events/*/cancel",  // POST
            "/api/calendar/events/*/export",  // GET (download)
            "/api/calendar/events/*/urls",    // GET
            "/api/calendar/export-all"        // GET (download)
    };

    private CalendarRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}