package be.steby.CoreProject.il.routes.debug;

/**
 * Security routes configuration for Debug/Test endpoints.
 *
 * ⚠️ CRITICAL: This entire file should be REMOVED or DISABLED in production!
 * Debug endpoints expose sensitive information and testing capabilities.
 */
public final class DebugRoutes {

    // ========== PUBLIC ROUTES ==========

    /**
     * ⚠️ DEV ONLY - Remove debug routes in production!
     * /error is needed for Spring Boot error handling.
     */
    public static final String[] PUBLIC = {
            "/api/debug/**",
            "/api/test/device-security/**",
            "/error"  // Spring Boot error page - must be public
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            // No authenticated routes
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * ⚠️ DEV ONLY - Remove in production!
     */
    public static final String[] CSRF_IGNORE = {
            "/api/debug/**",
            "/api/test/device-security/**",
            "/error"
    };

    private DebugRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}