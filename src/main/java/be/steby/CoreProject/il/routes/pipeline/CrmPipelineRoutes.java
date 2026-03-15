package be.steby.CoreProject.il.routes.pipeline;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM pipeline management domain.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/pipelines</pre>
 *
 * <h3>Access control</h3>
 * <p>Read routes are accessible to {@code COMMERCIAL} and {@code ADMIN} users.
 * Write routes are restricted to {@code ADMIN} only — enforced via
 * {@code @PreAuthorize} at the controller method level.</p>
 *
 * @see be.steby.CoreProject.pl.domains.pipeline.controllers.CrmPipelineController
 */
public final class CrmPipelineRoutes {

    private CrmPipelineRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/pipelines";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] READ_ROUTES = {
            BASE,           // GET — list all pipelines
            BASE + "/default", // GET — default pipeline
            BASE + "/**"    // GET — pipeline detail, steps list
    };

    private static final String[] WRITE_ROUTES = {
            BASE,                       // POST  — create pipeline
            BASE + "/*",                // PATCH, DELETE — update/delete pipeline
            BASE + "/*/steps",          // POST  — add step
            BASE + "/*/steps/reorder",  // PATCH — reorder steps
            BASE + "/*/steps/*"         // PATCH, DELETE — update/delete step
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN — reads + writes)
    // Fine-grained write restriction enforced via @PreAuthorize in controller
    // ========================================

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

    // Production version (uncomment and replace CSRF_IGNORE above)
    // public static final String[] CSRF_IGNORE = {};
}
