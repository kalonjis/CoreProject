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

    private static final String[] ADMIN_USER_ROUTES = {
            "/api/admin/users",                        // GET  — list all users
            "/api/admin/users/create",                 // POST — create user
            "/api/admin/users/all",                    // GET  — paginated user list
            "/api/admin/users/stats",                  // GET  — admin statistics
            "/api/admin/users/activate/**",            // PATCH — first-time activation
            "/api/admin/users/reactivate/**",          // PATCH — reactivation after deactivation
            "/api/admin/users/deactivate/**",          // PATCH — admin deactivation
            "/api/admin/users/delete/**",              // DELETE — hard delete (SUPER_ADMIN only)
            "/api/admin/users/gdpr/**",                // DELETE — GDPR anonymization (SUPER_ADMIN only)
            "/api/admin/users/grant-role/**",          // PATCH — grant role
            "/api/admin/users/revoke-role/**",         // PATCH — revoke role
            "/api/admin/users/deactivation-categories" // GET  — list deactivation categories
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

    private static final String[] TELEPHONY_MANAGEMENT = {
            "/api/admin/telephony/users",
            "/api/admin/telephony/users/**"
    };

    /**
     * All admin routes aggregated.
     */
    public static final String[] ADMIN = concatenate(
            ADMIN_USER_ROUTES,
            USER_ADDRESS_MANAGEMENT,
            ADDRESS_MANAGEMENT,
            DEVICE_MANAGEMENT,
            PASSWORD_MANAGEMENT,
            CACHE_DEVICE_MANAGEMENT,
            CACHE_USER_MANAGEMENT,
            SECURITY_LOGS,
            MONITORING,
            CIRCUIT_BREAKER,
            TELEPHONY_MANAGEMENT
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
    public static final String[] CSRF_IGNORE = {};

    private AdminRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}