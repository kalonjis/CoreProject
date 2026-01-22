package be.steby.CoreProject.il.routes.device;

/**
 * Security routes configuration for Device Management domain.
 */
public final class DeviceRoutes {

    // ========== PUBLIC ROUTES ==========

    /**
     * Public device routes for email-based confirmation only.
     *
     * ⚠️ NOTE: The old SecurityConstants had ALL device routes here for dev.
     * This is now corrected - only email token routes are truly public.
     */
    public static final String[] PUBLIC = {
            "/api/device/confirm",
            "/api/device/reject"
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            "/api/device/current",
            "/api/device/session",
            "/api/device/my-devices",
            "/api/device/request-confirmation",
            "/api/device/disconnect-all-others",
            "/api/device/trust-level/*",
            "/api/device/disconnect/*",
            "/api/device/*"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove ALL authenticated device routes!
     * Only keep: /confirm, /reject
     */
    public static final String[] CSRF_IGNORE = {
            // ✅ Safe - Email tokens, no cookies
            "/api/device/confirm",
            "/api/device/reject",

            // ⚠️ DEV ONLY - Remove in production (uses cookies)
            "/api/device/current",
            "/api/device/session",
            "/api/device/my-devices",
            "/api/device/request-confirmation",
            "/api/device/disconnect-all-others",
            "/api/device/trust-level/*",
            "/api/device/disconnect/*",
            "/api/device/*"
    };

    private DeviceRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}