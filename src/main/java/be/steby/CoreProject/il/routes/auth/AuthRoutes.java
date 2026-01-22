package be.steby.CoreProject.il.routes.auth;

/**
 * Security routes configuration for core Authentication operations.
 */
public final class AuthRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            "/api/auth/login",
            "/api/auth/initiate-login",
            "/api/auth/refresh-token"
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/session",
            "/api/auth/status"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove authenticated routes from this list!
     * Only keep: login, initiate-login, refresh-token
     */
    public static final String[] CSRF_IGNORE = {
            // ✅ Safe - No cookies, credentials in body
            "/api/auth/login",
            "/api/auth/initiate-login",
            "/api/auth/refresh-token",

            // ⚠️ DEV ONLY - Remove in production (uses cookies)
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/session",
            "/api/auth/status"
    };

    private AuthRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}