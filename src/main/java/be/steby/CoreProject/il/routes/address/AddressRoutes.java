package be.steby.CoreProject.il.routes.address;

/**
 * Security routes configuration for Address Management domain.
 */
public final class AddressRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            // No public routes - all address operations require authentication
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            "/api/profile/addresses",
            "/api/profile/addresses/*",
            "/api/profile/addresses/*/default",
            "/api/profile/addresses/*/primary",
            "/api/profile/addresses/*/type",
            "/api/profile/addresses/*/permanent"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: This should be EMPTY!
     * All address routes use cookies and need CSRF protection.
     */
    public static final String[] CSRF_IGNORE = {
            // ⚠️ DEV ONLY - Remove ALL in production (uses cookies)
            "/api/profile/addresses",
            "/api/profile/addresses/*",
            "/api/profile/addresses/*/default",
            "/api/profile/addresses/*/primary",
            "/api/profile/addresses/*/type",
            "/api/profile/addresses/*/permanent"
    };

    private AddressRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}