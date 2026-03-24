package be.steby.CoreProject.il.routes.deal;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM deal management domain.
 *
 * <p>Defines all API endpoints related to deal CRUD, stage transitions,
 * and commercial reassignment, accessible only to users with {@code COMMERCIAL}
 * or {@code ADMIN} authority.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/deals</pre>
 *
 * @see be.steby.CoreProject.pl.domains.deal.controllers.CrmDealController
 */
public final class CrmDealRoutes {

    private CrmDealRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/deals";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] LIST_ROUTES = {
            BASE,        // GET  — paginated, filtered deal list
            BASE + "/**" // GET  — deal detail by publicId
    };

    private static final String[] WRITE_ROUTES = {
            BASE,                         // POST  — create deal
            BASE + "/*",                  // PATCH — update deal by publicId
            BASE + "/*/stage",            // PATCH — move deal to a different stage
            BASE + "/*/assign",           // PATCH — reassign/unassign commercial
            BASE + "/*/contacts",         // POST  — add contact to deal
            BASE + "/*/contacts/**"       // DELETE/PATCH — remove contact or update role/primary
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM deal routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
     */
    public static final String[] COMMERCIAL = concatenate(LIST_ROUTES, WRITE_ROUTES);

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
    public static final String[] CSRF_IGNORE = concatenate(LIST_ROUTES, WRITE_ROUTES);

    // Production version (uncomment and replace CSRF_IGNORE above)
    // public static final String[] CSRF_IGNORE = {};
}
