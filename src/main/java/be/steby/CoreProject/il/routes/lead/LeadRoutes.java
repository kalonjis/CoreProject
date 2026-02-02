package be.steby.CoreProject.il.routes.lead;

/**
 * Route definitions for inquiry domain.
 *
 * <p>Defines all API endpoints related to public inquiry operations
 * for anonymous visitors.</p>
 */
public final class LeadRoutes {

    private LeadRoutes() {
        // Utility class
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/lead";

    // ========================================
    // Public Routes (accessible without authentication)
    // ========================================

    /**
     * Routes accessible without authentication.
     * Public inquiry form is accessible to everyone.
     */
    public static final String[] PUBLIC = {
            BASE,           // POST - Submit inquiry
            BASE + "/**"    // Future endpoints
    };

    // ========================================
    // CSRF Configuration
    // ========================================

    /**
     * Routes where CSRF protection should be ignored.
     * Empty - CSRF protection is ENABLED for form submissions.
     */
    public static final String[] CSRF_IGNORE = {
            "/api/lead"
    };
}