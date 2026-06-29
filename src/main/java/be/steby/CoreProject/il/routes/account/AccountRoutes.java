package be.steby.CoreProject.il.routes.account;

/**
 * Security routes configuration for Account Management domain.
 */
public final class AccountRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            "/api/account/signup",
            "/api/account/activate/**",
            "/api/account/resend-activation/**",
            "/api/account/resend-activation-by-identifier",
            "/api/account/request-reactivation",
            "/api/account/confirm-reactivation/**",
            "/api/account/confirm-deletion"
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            "/api/account/request-deactivation",
            "/api/account/confirm-deactivation/**",
            "/api/account/request-deletion"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove request-deactivation and confirm-deactivation!
     */
    public static final String[] CSRF_IGNORE = {
            "/api/account/signup",
            "/api/account/activate/**",
            "/api/account/resend-activation/**",
            "/api/account/resend-activation-by-identifier",
            "/api/account/request-reactivation",
            "/api/account/confirm-reactivation/**",
            "/api/account/confirm-deletion"
    };

    private AccountRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}