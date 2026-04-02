package be.steby.CoreProject.il.routes.supportticket;

/**
 * Route definitions for the public support ticket submission endpoint.
 *
 * <p>This endpoint is accessible without authentication — it handles support
 * ticket submissions from anonymous visitors on the public contact form.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/public/support</pre>
 *
 * @see be.steby.CoreProject.pl.domains.supportticket.controllers.PublicSupportTicketController
 */
public final class PublicSupportTicketRoutes {

    private PublicSupportTicketRoutes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    // ========================================
    // Base Path
    // ========================================

    public static final String BASE = "/api/public/support";

    // ========================================
    // Public Routes (no authentication required)
    // ========================================

    public static final String[] PUBLIC = {
            BASE     // POST — submit a support ticket
    };

    // ========================================
    // CSRF Configuration
    // ========================================

    /**
     * CSRF disabled for this endpoint — public form submission from any origin.
     *
     * <p>⚠️ TODO PRODUCTION: evaluate whether to keep this or enforce CSRF via
     * a double-submit cookie pattern.</p>
     */
    public static final String[] CSRF_IGNORE = {
            BASE
    };
}
