package be.steby.CoreProject.il.routes.password;

/**
 * Security routes configuration for Password Management domain.
 */
public final class PasswordRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            "/api/password/forgot/email-link",
            "/api/password/forgot/email-code",
            "/api/password/forgot/sms-code",
            "/api/password/verify-code",
            "/api/password/reset",
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
     * Public token-based routes bypass CSRF (no session cookie involved).
     *
     * ⚠️ TODO PRODUCTION: Remove /change and /define!
     */
    public static final String[] CSRF_IGNORE = {
            "/api/password/forgot/email-link",
            "/api/password/forgot/email-code",
            "/api/password/forgot/sms-code",
            "/api/password/verify-code",
            "/api/password/reset",
            "/api/password/reset-with-permission",
            "/api/password/reset/resend"
    };

    private PasswordRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}