package be.steby.CoreProject.il.routes.tag;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM tag management domain.
 *
 * <p>Tags are global labels applied to {@code Contact}s and {@code Deal}s.
 * CRUD operations are accessible to users with {@code COMMERCIAL} or {@code ADMIN}
 * authority. Deletion is additionally restricted to {@code ADMIN} at the
 * method level via {@code @PreAuthorize} in the controller.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/tags</pre>
 *
 * @see be.steby.CoreProject.pl.domains.tag.controllers.CrmTagController
 */
public final class CrmTagRoutes {

    private CrmTagRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/tags";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] LIST_ROUTES = {
            BASE,        // GET  — list all tags
            BASE + "/**" // GET/POST/PATCH/DELETE — tag detail and associations
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM tag routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
     *
     * <p>Fine-grained authorization (e.g. DELETE restricted to ADMIN)
     * is enforced at the controller method level via {@code @PreAuthorize}.</p>
     */
    public static final String[] COMMERCIAL = LIST_ROUTES;

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
