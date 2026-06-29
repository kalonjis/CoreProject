package be.steby.CoreProject.il.routes.supportticket;

/**
 * Route definitions for the CRM support ticket domain.
 *
 * <p>Exposes endpoints for creating, reading, updating, and managing
 * the lifecycle of support tickets linked to CRM contacts.
 * Only users with {@code COMMERCIAL} or {@code ADMIN} authority may access
 * these endpoints.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/support-tickets</pre>
 *
 * @see be.steby.CoreProject.pl.domains.supportticket.controllers.CrmSupportTicketController
 */
public final class CrmSupportTicketRoutes {

    private CrmSupportTicketRoutes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/support-tickets";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] ALL_ROUTES = {
            BASE,           // GET (list), POST (create)
            BASE + "/**"    // GET (detail), PATCH (update, status, assign)
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM support ticket routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
     */
    public static final String[] COMMERCIAL = ALL_ROUTES;

    // ========================================
    // CSRF Configuration
    // ========================================

    /**
     * Routes that bypass CSRF protection.
     *
     * <p>⚠️ DEV ONLY — remove all in production.</p>
     *
     * <p>⚠️ TODO PRODUCTION: Set this to empty array: {@code {}}</p>
     */
    public static final String[] CSRF_IGNORE = {};
}
