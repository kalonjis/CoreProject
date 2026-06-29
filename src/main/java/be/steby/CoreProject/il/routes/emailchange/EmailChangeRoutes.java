package be.steby.CoreProject.il.routes.emailchange;

/**
 * Security routes configuration for Email Change domain.
 */
public final class EmailChangeRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            "/api/email-address-change/cancel",
            "/api/email-address-change/verification",
            "/api/email-address-change/confirmation"
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            "/api/email-address-change/request"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove /request!
     */
    public static final String[] CSRF_IGNORE = {
            "/api/email-address-change/cancel",
            "/api/email-address-change/verification",
            "/api/email-address-change/confirmation"
    };

    private EmailChangeRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}