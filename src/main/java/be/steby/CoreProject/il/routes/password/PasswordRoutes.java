package be.steby.CoreProject.il.routes.password;

/**
 * Security routes configuration for Password Management domain.
 */
public final class PasswordRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            "/api/password/forgot",
            "/api/password/reset",
            "/api/password/verify-code",
            "/api/password/reset-with-permission",
            "/api/password/reset/resend"
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            "/api/password/change",
            "/api/password/define"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove /change and /define!
     */
    public static final String[] CSRF_IGNORE = {
            // ✅ Safe - Email tokens, no cookies
            "/api/password/forgot",
            "/api/password/reset",
            "/api/password/reset/resend",
            "/api/password/verify-code",
            "/api/password/reset-with-permission",

            // ⚠️ DEV ONLY - Remove in production (uses cookies)
            "/api/password/change",
            "/api/password/define"
    };

    private PasswordRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}