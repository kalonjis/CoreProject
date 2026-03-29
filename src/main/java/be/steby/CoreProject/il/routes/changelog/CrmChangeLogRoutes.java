package be.steby.CoreProject.il.routes.changelog;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM change log domain.
 *
 * <p>Exposes the read-only audit trail of field changes on CRM entities.
 * Only users with {@code COMMERCIAL} or {@code ADMIN} authority may access
 * these endpoints.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/changelog</pre>
 *
 * @see be.steby.CoreProject.pl.domains.changelog.controllers.CrmChangeLogController
 */
public final class CrmChangeLogRoutes {

    private CrmChangeLogRoutes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/changelog";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] READ_ROUTES = {
            BASE + "/**"   // GET — change history for any CRM entity
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM change log routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
     */
    public static final String[] COMMERCIAL = READ_ROUTES;

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
    public static final String[] CSRF_IGNORE = READ_ROUTES;

    // Production version (uncomment and replace CSRF_IGNORE above)
    // public static final String[] CSRF_IGNORE = {};
}
