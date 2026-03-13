package be.steby.CoreProject.il.routes.contact;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM contact management domain.
 *
 * <p>Defines all API endpoints related to contact CRUD, lifecycle transitions,
 * and merge operations, accessible only to users with {@code COMMERCIAL}
 * or {@code ADMIN} authority.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/contacts</pre>
 *
 * @see be.steby.CoreProject.pl.domains.contact.controllers.CrmContactController
 */
public final class CrmContactRoutes {

    private CrmContactRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/contacts";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] LIST_ROUTES = {
            BASE,        // GET  — paginated, filtered contact list
            BASE + "/**" // GET  — contact detail by publicId
    };

    private static final String[] WRITE_ROUTES = {
            BASE,                        // POST  — create contact
            BASE + "/merge",             // POST  — merge duplicate contacts
            BASE + "/*",                 // PATCH — update contact by publicId
            BASE + "/*/status",          // PATCH — update CRM status
            BASE + "/*/organisation",    // PATCH — link/unlink organisation
            BASE + "/*/assign"           // PATCH — assign/unassign commercial
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM contact routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
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
