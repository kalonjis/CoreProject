package be.steby.CoreProject.il.routes.auth;

/**
 * Security routes configuration for Two-Factor Authentication (2FA).
 */
public final class TwoFactorRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            "/api/auth/verify-2fa",
            "/api/auth/resend-2fa-code",
            "/api/auth/2fa-status",
            "/api/auth/2fa/choose-method",
            "/api/auth/2fa/login/methods"
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            // General 2FA Settings
            "/api/auth/2fa/settings",
            "/api/auth/2fa/available-methods",

            // Email 2FA
            "/api/auth/2fa/email/enable",
            "/api/auth/2fa/email/setup/initiate",
            "/api/auth/2fa/email/setup/verify",
            "/api/auth/2fa/email/disable",

            // SMS 2FA
            "/api/auth/2fa/sms/enable",
            "/api/auth/2fa/sms/disable",
            "/api/auth/2fa/sms/setup/initiate",
            "/api/auth/2fa/sms/setup/verify",

            // TOTP (Authenticator App) 2FA
            "/api/auth/2fa/totp/enable",
            "/api/auth/2fa/totp/disable",
            "/api/auth/2fa/totp/test-code",
            "/api/auth/2fa/totp/setup/initiate",
            "/api/auth/2fa/totp/setup/verify",

            // Backup Codes 2FA
            "/api/auth/2fa/backup-codes/enable",
            "/api/auth/2fa/backup-codes/disable",

            // TODO: Move to ProfileRoutes
            "/api/profile/SMS/request-verification",
            "/api/profile/SMS/verify"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove all AUTHENTICATED routes from this list!
     * Only keep the PUBLIC routes.
     */
    public static final String[] CSRF_IGNORE = {
            // ✅ Safe - Login phase, temporary tokens, no cookies
            "/api/auth/verify-2fa",
            "/api/auth/resend-2fa-code",
            "/api/auth/2fa-status",
            "/api/auth/2fa/choose-method",
            "/api/auth/2fa/login/methods",
            "/api/auth/2fa/methods",  // Alias

            // ⚠️ DEV ONLY - Remove in production (uses cookies)
            "/api/auth/2fa/settings",
            "/api/auth/2fa/available-methods",
            "/api/auth/2fa/email/setup/initiate",
            "/api/auth/2fa/email/setup/verify",
            "/api/auth/2fa/enable-email",  // Legacy route
            "/api/auth/2fa/email/enable",
            "/api/auth/2fa/email/disable",
            "/api/auth/2fa/sms/enable",
            "/api/auth/2fa/sms/disable",
            "/api/auth/2fa/totp/enable",
            "/api/auth/2fa/totp/disable",
            "/api/auth/2fa/totp/test-code",
            "/api/auth/2fa/totp/setup/initiate",
            "/api/auth/2fa/totp/setup/verify",
            "/api/auth/2fa/backup-codes/enable",
            "/api/auth/2fa/backup-codes/disable",
            "/api/profile/SMS/request-verification",
            "/api/profile/SMS/verify",
            "/api/auth/2fa/sms/setup/initiate",
            "/api/auth/2fa/sms/setup/verify"

    };

    private TwoFactorRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}