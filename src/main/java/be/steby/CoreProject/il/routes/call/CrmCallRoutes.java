package be.steby.CoreProject.il.routes.call;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM call session domain.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/calls</pre>
 *
 * @see be.steby.CoreProject.pl.domains.call.controllers.CrmCallController
 */
public final class CrmCallRoutes {

    private CrmCallRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/calls";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] READ_ROUTES = {
            BASE + "/*"   // GET /{publicId}
    };

    private static final String[] WRITE_ROUTES = {
            BASE,         // POST — initiate call
            BASE + "/**"  // PATCH /{publicId}/terminate
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM call routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
     */
    public static final String[] COMMERCIAL = concatenate(READ_ROUTES, WRITE_ROUTES);

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
    public static final String[] CSRF_IGNORE = concatenate(READ_ROUTES, WRITE_ROUTES);
}
