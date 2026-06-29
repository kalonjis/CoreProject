package be.steby.CoreProject.il.routes.organisation;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM organisation management domain.
 *
 * <p>Defines all API endpoints related to organisation CRUD and merge operations,
 * accessible only to users with {@code COMMERCIAL} or {@code ADMIN} authority.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/organisations</pre>
 *
 * @see be.steby.CoreProject.pl.domains.organisation.controllers.CrmOrganisationController
 */
public final class CrmOrganisationRoutes {

    private CrmOrganisationRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/organisations";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] LIST_ROUTES = {
            BASE,          // GET  — paginated, filtered organisation list
            BASE + "/**"   // GET  — organisation detail by publicId
    };

    private static final String[] WRITE_ROUTES = {
            BASE,              // POST  — create organisation
            BASE + "/merge",   // POST  — merge duplicate organisations
            BASE + "/*"        // PATCH — update organisation by publicId
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM organisation routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
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
