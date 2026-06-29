package be.steby.CoreProject.il.routes.lead;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Route definitions for the CRM lead management domain.
 *
 * <p>Defines all API endpoints related to the commercial lifecycle of a lead,
 * accessible only to users with {@code COMMERCIAL} or {@code ADMIN} authority.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/leads</pre>
 *
 * @see be.steby.CoreProject.pl.domains.lead.controllers.CrmLeadController
 */
public final class CrmLeadRoutes {

    private CrmLeadRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/crm/leads";

    // ========================================
    // Route Segments
    // ========================================

    private static final String[] QUEUE_ROUTES = {
            BASE,               // GET  — paginated, filtered lead list
            BASE + "/**"        // GET  — lead detail by publicId
    };

    private static final String[] LIFECYCLE_ROUTES = {
            BASE + "/*/enrich",  // PATCH — enrich lead with contact details
            BASE + "/*/assign",  // PATCH — assign lead to a commercial
            BASE + "/*/review",  // PATCH — transition lead to IN_REVIEW
            BASE + "/*/convert", // POST  — convert lead to Contact
            BASE + "/*/reject"   // PATCH — reject lead with reason
    };

    // ========================================
    // Commercial Routes (COMMERCIAL or ADMIN authority required)
    // ========================================

    /**
     * All CRM lead routes — require {@code COMMERCIAL} or {@code ADMIN} authority.
     */
    public static final String[] COMMERCIAL = concatenate(
            QUEUE_ROUTES,
            LIFECYCLE_ROUTES
    );

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