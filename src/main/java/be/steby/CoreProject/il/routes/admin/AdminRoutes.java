package be.steby.CoreProject.il.routes.admin;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Security routes configuration for Admin domain.
 */
public final class AdminRoutes {

    // ========== PUBLIC ROUTES ==========

    public static final String[] PUBLIC = {
            // No public routes for admin
    };

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            // No regular authenticated routes - all require ADMIN role
    };

    // ========== ADMIN ROUTES ==========

    private static final String[] USER_MANAGEMENT = {
            "/api/admin/users",
            "/api/admin/users/create",
            "/api/admin/users/all",
            "/api/admin/users/stats",
            "/api/admin/users/activate/**",
            "/api/admin/users/deactivate/**",
            "/api/admin/users/force-reset-password/**",
            "/api/admin/users/grant-role/**",
            "/api/admin/users/revoke-role/**",
            "/api/admin/users/gdpr-deletion/**",
            "/api/admin/users/deactivation-categories"
    };

    private static final String[] USER_ADDRESS_MANAGEMENT = {
            "/api/admin/users/*/addresses",
            "/api/admin/users/*/addresses/*/metadata",
            "/api/admin/users/*/addresses/**",
            "/api/admin/users/*/addresses/*/permanent"
    };

    private static final String[] ADDRESS_MANAGEMENT = {
            "/api/admin/addresses",
            "/api/admin/addresses/*",
            "/api/admin/addresses/*/users"
    };

    private static final String[] DEVICE_MANAGEMENT = {
            "/api/admin/device/list/user/**",
            "/api/admin/device/list/**"
    };

    private static final String[] PASSWORD_MANAGEMENT = {
            "/api/admin/password-reset/**",
            "/api/admin/password-reset/send-reset-link/**",
            "/api/admin/password-reset/send-temporary-password/**",
            "/api/admin/password-reset/send-via-alternative-channel/**"
    };

    private static final String[] CACHE_DEVICE_MANAGEMENT = {
            "/api/admin/cache/device/stats",
            "/api/admin/cache/device/cleanup",
            "/api/admin/cache/device/**"
    };

    private static final String[] CACHE_USER_MANAGEMENT = {
            "/api/admin/cache/user/stats",
            "/api/admin/cache/user/health",
            "/api/admin/cache/user/cleanup",
            "/api/admin/cache/user/maintenance",
            "/api/admin/cache/user/username/**",
            "/api/admin/cache/user/id/**",
            "/api/admin/cache/user/role/**",
            "/api/admin/cache/user/security-incident",
            "/api/admin/cache/user/config"
    };

    private static final String[] SECURITY_LOGS = {
            "/api/security/logs/user/**"
    };

    private static final String[] MONITORING = {
            "/api/admin/monitoring/health",
            "/api/admin/monitoring/health/**",
            "/api/admin/monitoring/metrics/**",
            "/api/admin/monitoring/circuit-breakers"
    };

    private static final String[] CIRCUIT_BREAKER = {
            "/api/admin/circuit-breaker/**"
    };

    /**
     * All admin routes aggregated.
     */
    public static final String[] ADMIN = concatenate(
            USER_MANAGEMENT,
            USER_ADDRESS_MANAGEMENT,
            ADDRESS_MANAGEMENT,
            DEVICE_MANAGEMENT,
            PASSWORD_MANAGEMENT,
            CACHE_DEVICE_MANAGEMENT,
            CACHE_USER_MANAGEMENT,
            SECURITY_LOGS,
            MONITORING,
            CIRCUIT_BREAKER
    );

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ CRITICAL: In production this MUST be EMPTY!
     * Admin routes should NEVER bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Set this to empty array: {}
     */
    public static final String[] CSRF_IGNORE = concatenate(
            // ⚠️ DEV ONLY - Remove ALL in production!
            USER_MANAGEMENT,
            USER_ADDRESS_MANAGEMENT,
            ADDRESS_MANAGEMENT,
            DEVICE_MANAGEMENT,
            PASSWORD_MANAGEMENT,
            CACHE_DEVICE_MANAGEMENT,
            CACHE_USER_MANAGEMENT,
            SECURITY_LOGS,
            MONITORING,
            CIRCUIT_BREAKER
    );

    // Production version (uncomment and replace CSRF_IGNORE above)
    // public static final String[] CSRF_IGNORE = {};

    private AdminRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}