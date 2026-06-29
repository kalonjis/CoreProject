package be.steby.CoreProject.il.routes.debug;

/**
 * Security routes for Spring Boot infrastructure endpoints.
 */
public final class DebugRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            "/error"  // Spring Boot error page - must be public
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            // No authenticated routes
    };

    // ========== CSRF CONFIGURATION ==========

    public static final String[] CSRF_IGNORE = {
            "/error"
    };

    private DebugRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}