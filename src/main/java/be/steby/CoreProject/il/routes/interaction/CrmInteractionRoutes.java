package be.steby.CoreProject.il.routes.interaction;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM interaction management domain.
 *
 * <p>Defines all API endpoints related to interaction logging, timeline retrieval,
 * partial updates, and deletion, accessible only to users with {@code COMMERCIAL}
 * or {@code ADMIN} authority.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/interactions</pre>
 *
 * @see be.steby.CoreProject.pl.domains.interaction.controllers.CrmInteractionController
 */
public final class CrmInteractionRoutes {

    private CrmInteractionRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/interactions";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] LIST_ROUTES = {
            BASE,        // GET  — interaction detail by publicId (via /{publicId})
            BASE + "/**" // GET  — deal timeline, contact timeline
    };

    private static final String[] WRITE_ROUTES = {
            BASE,        // POST  — log new interaction
            BASE + "/*"  // PATCH — update, DELETE — delete by publicId
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM interaction routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
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
