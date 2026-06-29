package be.steby.CoreProject.il.routes.commercialaction;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM commercial action management domain.
 *
 * <p>Defines all API endpoints related to commercial action creation, lifecycle
 * transitions (complete, cancel), and commercial reassignment, accessible only
 * to users with {@code COMMERCIAL} or {@code ADMIN} authority.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/commercial-actions</pre>
 *
 * @see be.steby.CoreProject.pl.domains.commercialaction.controllers.CrmCommercialActionController
 */
public final class CrmCommercialActionRoutes {

    private CrmCommercialActionRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/commercial-actions";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] LIST_ROUTES = {
            BASE,        // GET  — list (my, by deal, by contact)
            BASE + "/my",// GET  — my actions view
            BASE + "/**" // GET  — action detail by publicId, deal/contact sub-paths
    };

    private static final String[] WRITE_ROUTES = {
            BASE,                    // POST  — create commercial action
            BASE + "/*",             // PATCH — update action by publicId
            BASE + "/*/complete",    // PATCH — mark action as DONE
            BASE + "/*/cancel",      // PATCH — mark action as CANCELLED
            BASE + "/*/assign"       // PATCH — reassign commercial
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM commercial action routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
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
    public static final String[] CSRF_IGNORE = {};
}
