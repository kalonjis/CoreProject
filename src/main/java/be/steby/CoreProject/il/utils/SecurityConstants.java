package be.steby.CoreProject.il.utils;

/**
 * Centralized security constants for route configuration.
 *
 * <p>CRITICAL SECURITY NOTE:
 * This application uses HttpOnly cookies for authentication (access + refresh tokens).
 * Therefore, CSRF protection MUST be active for ALL authenticated routes.
 *
 * <p>CSRF_IGNORE should ONLY include:
 * - Public routes (no authentication required)
 * - Routes using tokens from request body/URL (not cookies)
 */
public class SecurityConstants {

    // ========== AUTH DOMAIN ==========

    private static final String[] AUTH_PUBLIC_ROUTES = {
            "/api/auth/login",
            "/api/auth/initiate-login",
            "/api/auth/verify-2fa",
            "/api/auth/resend-2fa-code",
            "/api/auth/2fa-status",
            "/api/auth/refresh-token",
            "/api/auth/2fa/choose-method"
    };

    private static final String[] AUTH_AUTHENTICATED_ROUTES = {
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/status",
            "/api/auth/2fa/email/enable",
            "/api/auth/2fa/email/disable",
            "/api/auth/2fa/sms/enable",
            "/api/auth/2fa/sms/disable",
            "/api/auth/2fa/totp/enable",
            "/api/auth/2fa/totp/disable",
            "/api/auth/2fa/backup-codes/enable",
            "/api/auth/2fa/backup-codes/disable",
            "/api/auth/2fa/webauthn/initiate-activation",
            "/api/auth/2fa/available-methods",
            //TODO move this into profile routes...
            "/api/profile/SMS/request-verification",
            "/api/profile/SMS/verify"


    };

    /**
     * CSRF ignored only for login/register (no prior authentication).
     * refresh-token and logout keep CSRF protection (use cookies).
     */
    private static final String[] AUTH_CSRF_IGNORE = {
            "/api/auth/login",
            "/api/auth/initiate-login",
            "/api/auth/verify-2fa",
            "/api/auth/resend-2fa-code",
            "/api/auth/2fa-status",
            "/api/auth/refresh-token",
            //TODO delete "me", "status" and "logout" from this list in prod
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/status",
            "/api/auth/2fa/enable-email",
            "/api/auth/2fa/email/enable",
            "/api/auth/2fa/email/disable",
            "/api/auth/2fa/sms/enable",
            "/api/auth/2fa/sms/disable",
            "/api/auth/2fa/totp/enable",
            "/api/auth/2fa/totp/disable",
            "/api/auth/2fa/backup-codes/enable",
            "/api/auth/2fa/backup-codes/disable",
            "/api/auth/2fa/webauthn/initiate-activation",
            "/api/auth/2fa/available-methods",
            "/api/auth/2fa/choose-method",
            //TODO move this into profile routes...
            "/api/profile/SMS/request-verification",
            "/api/profile/SMS/verify"
    };

    // ========== ACCOUNT DOMAIN ==========

    private static final String[] ACCOUNT_PUBLIC_ROUTES = {
            "/api/account/signup",
            "/api/account/activate/**",
            "/api/account/resend-activation/**",
            "/api/account/request-reactivation",
            "/api/account/confirm-reactivation/**"
    };

    private static final String[] ACCOUNT_AUTHENTICATED_ROUTES = {
            "/api/account/request-deactivation",
            "/api/account/confirm-deactivation/**"
    };

    /**
     * CSRF ignored ONLY for public routes (token-based).
     * Authenticated routes keep CSRF protection (cookies).
     */
    private static final String[] ACCOUNT_CSRF_IGNORE = {
            "/api/account/signup",
            "/api/account/activate/**",
            "/api/account/resend-activation/**",
            "/api/account/request-reactivation",
            "/api/account/confirm-reactivation/**",
            // TODO remove "request-deactivation", "confirm-deactivation" in prod
            "/api/account/request-deactivation",
            "/api/account/confirm-deactivation/**"
    };

    // ========== PASSWORD DOMAIN ==========

    private static final String[] PASSWORD_PUBLIC_ROUTES = {
            "/api/password/forgot",
            "/api/password/reset",
            "/api/password/reset/resend"
    };

    private static final String[] PASSWORD_AUTHENTICATED_ROUTES = {
            "/api/password/change"
    };

    /**
     * CSRF ignored ONLY for public routes (token-based).
     * /api/password/change keeps CSRF protection (cookies).
     */
    private static final String[] PASSWORD_CSRF_IGNORE = {
            "/api/password/forgot",
            "/api/password/reset",
            "/api/password/reset/resend",
            // TODO delete "change" from passwordcsrfignore in prod
            "/api/password/change"
    };

    // ========== EMAIL CHANGE DOMAIN ==========

    private static final String[] EMAIL_CHANGE_PUBLIC_ROUTES = {
            "/api/email-address-change/cancel",
            "/api/email-address-change/verification",
            "/api/email-address-change/confirmation"
    };

    private static final String[] EMAIL_CHANGE_CSRF_IGNORE = {
            "/api/email-address-change/cancel",
            "/api/email-address-change/verification",
            "/api/email-address-change/confirmation",
            // TODO remove "change/request" in prod
            "/api/email-address-change/request"
    };
    /**
     * All email change routes require authentication and use cookies.
     * CSRF protection ACTIVE for all routes (not in CSRF_IGNORE).
     */
    private static final String[] EMAIL_CHANGE_AUTHENTICATED_ROUTES = {
            "/api/email-address-change/request",

    };

    // No EMAIL_CHANGE_CSRF_IGNORE - all routes keep CSRF protection ✅

    // ========== DEVICE DOMAIN ==========

    /**
     * Public device routes (no authentication required)
     * Used for email confirmation links - users click these in emails
     */
    private static final String[] DEVICE_PUBLIC_ROUTES = {
            "/api/device/confirm",      // GET - confirm device via email token
            "/api/device/reject",        // GET - reject device via email token
            //todo : removed these url from public routes in prod
            "/api/device/current",                    // GET - current device info
            "/api/device/my-devices",                 // GET - all user's devices list
            "/api/device/request-confirmation",       // POST - request confirmation link
            "/api/device/disconnect-all-others",      // POST - disconnect all other devices
            "/api/device/trust-level/*",              // PATCH - update device trust level by ID
            "/api/device/disconnect/*",               // POST - disconnect specific device by ID
            "/api/device/*"
    };

    /**
     * Authenticated device routes (require user authentication)
     * All device management operations for authenticated users
     */
    private static final String[] DEVICE_AUTHENTICATED_ROUTES = {
            "/api/device/current",                    // GET - current device info
            "/api/device/my-devices",                 // GET - all user's devices list
            "/api/device/request-confirmation",       // POST - request confirmation link
            "/api/device/disconnect-all-others",      // POST - disconnect all other devices
            "/api/device/trust-level/*",              // PATCH - update device trust level by ID
            "/api/device/disconnect/*",               // POST - disconnect specific device by ID
            "/api/device/*"                           // GET - specific device info by ID
    };

    /**
     * CSRF ignored ONLY for public routes (token-based).
     * Authenticated device routes keep CSRF protection (cookies).
     */
    private static final String[] DEVICE_CSRF_IGNORE = DEVICE_PUBLIC_ROUTES;

    // ========== ADMIN DOMAIN ==========

    // ========== ADMIN DOMAIN ==========

    /**
     * Administrative routes requiring ADMIN or SUPER_ADMIN authority.
     *
     * ⚠️ SECURITY: All routes explicitly listed to prevent accidental exposure.
     * Never use wildcards like /api/admin/** for security-critical routes.
     */

    private static final String[] ADMIN_USER_ROUTES = {
            "/api/admin/users",                           // GET all users, POST create user
            "/api/admin/users/all",                       // GET paginated users
            "/api/admin/users/stats",                     // GET admin statistics
            "/api/admin/users/activate/**",               // PATCH activate user
            "/api/admin/users/deactivate/**",             // PATCH deactivate user
            "/api/admin/users/force-reset-password/**",   // POST force password reset
            "/api/admin/users/grant-role/**",             // PATCH grant role
            "/api/admin/users/revoke-role/**",            // PATCH revoke role
            "/api/admin/users/gdpr-deletion/**",          // DELETE GDPR deletion
            "/api/admin/users/deactivation-categories"    // GET deactivation categories
    };


    private static final String[] ADMIN_PASSWORD_ROUTES = {
            "/api/admin/password-reset/**",            // POST force password reset
            "/api/admin/password-reset/send-reset-link/**",  // POST password reset
            "/api/admin/password-reset/send-temporary-password/**",  // POST password reset
            "/api/admin/password-reset/send-via-alternative-channel/**"  // POST password reset
            // Future routes will be under same base path
    };

    // Admin Cache Management - Device
    private static final String[] ADMIN_CACHE_DEVICE_ROUTES = {
            "/api/admin/cache/device/stats",              // GET cache stats
            "/api/admin/cache/device/cleanup",            // POST cleanup
            "/api/admin/cache/device/**"                  // DELETE invalidate device
    };

    // Admin Cache Management - User
    private static final String[] ADMIN_CACHE_USER_ROUTES = {
            "/api/admin/cache/user/stats",                // GET cache stats
            "/api/admin/cache/user/health",               // GET cache health
            "/api/admin/cache/user/cleanup",              // POST cleanup
            "/api/admin/cache/user/maintenance",          // POST maintenance
            "/api/admin/cache/user/username/**",          // DELETE invalidate by username
            "/api/admin/cache/user/id/**",                // DELETE invalidate by id
            "/api/admin/cache/user/role/**",              // DELETE invalidate by role
            "/api/admin/cache/user/security-incident",    // POST security incident
            "/api/admin/cache/user/config"                // GET cache config
    };

    // Admin Security Logs
    private static final String[] ADMIN_SECURITY_ROUTES = {
            "/api/security/logs/user/**"                  // GET user security logs
    };

    // Admin Device Management
    private static final String[] ADMIN_DEVICE_ROUTES = {
            "/api/admin/device/list/user/**",              // GET devices by user
            "/api/admin/device/list/**"              // GET devices by user
    };

    /**
     * Aggregated admin routes.
     * All routes require ADMIN or SUPER_ADMIN authority.
     */
    private static final String[] ADMIN_DOMAIN_ROUTES = concatenate(
            ADMIN_USER_ROUTES,
            ADMIN_CACHE_DEVICE_ROUTES,
            ADMIN_CACHE_USER_ROUTES,
            ADMIN_SECURITY_ROUTES,
            ADMIN_DEVICE_ROUTES,
            ADMIN_PASSWORD_ROUTES
    );

    /**
     * CSRF protection ACTIVE for ALL admin routes.
     * Admin operations are state-changing and must be protected.
     *
     * ⚠️ SECURITY: Admin routes are NEVER in CSRF_IGNORE.
     */
// No ADMIN_CSRF_IGNORE defined - all routes keep CSRF protection ✅

    // ✅ No ADMIN_CSRF_IGNORE - all admin routes keep CSRF protection

    // ========== SWAGGER / DEBUG ==========

    private static final String[] SWAGGER_ROUTES = {
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    /**
     * Debug routes - should be disabled in production.
     */
    private static final String[] DEBUG_ROUTES = {
            "/api/debug/**",
            "/api/test/device-security/**"
    };

    // ========== AGGREGATED CONSTANTS ==========

    /**
     * All routes accessible without authentication.
     */
    public static final String[] PUBLIC_ROUTES = concatenate(
            AUTH_PUBLIC_ROUTES,
            ACCOUNT_PUBLIC_ROUTES,
            PASSWORD_PUBLIC_ROUTES,
            EMAIL_CHANGE_PUBLIC_ROUTES,
            DEVICE_PUBLIC_ROUTES,
            SWAGGER_ROUTES,
            DEBUG_ROUTES
    );

    /**
     * Routes requiring authentication but no specific role.
     * These routes use cookies and have CSRF protection (not in CSRF_IGNORE).
     */
    public static final String[] AUTHENTICATED_ROUTES = concatenate(
            AUTH_AUTHENTICATED_ROUTES,
            ACCOUNT_AUTHENTICATED_ROUTES,
            PASSWORD_AUTHENTICATED_ROUTES,
            EMAIL_CHANGE_AUTHENTICATED_ROUTES,
            DEVICE_AUTHENTICATED_ROUTES
    );

    /**
     * Routes requiring ADMIN or SUPER_ADMIN authority.
     * All admin routes have CSRF protection (not in CSRF_IGNORE).
     */
    public static final String[] ADMIN_ROUTES = ADMIN_DOMAIN_ROUTES;

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ CRITICAL SECURITY PRINCIPLES:
     * <ul>
     *   <li>ONLY public routes that don't use cookies</li>
     *   <li>NEVER include AUTHENTICATED_ROUTES (cookies = CSRF risk)</li>
     *   <li>NEVER include ADMIN_ROUTES (highly sensitive operations)</li>
     * </ul>
     *
     * <p>This application uses HttpOnly cookies for authentication.
     * All authenticated routes MUST have CSRF protection.
     */
    public static final String[] CSRF_IGNORE_PATHS = concatenate(
            AUTH_CSRF_IGNORE,        // Only public routes
            ACCOUNT_CSRF_IGNORE,     // Only public routes
            ADMIN_DOMAIN_ROUTES,// TODO delete in prod
            PASSWORD_CSRF_IGNORE,    // Only public routes
            DEVICE_CSRF_IGNORE,      // Only public routes
            EMAIL_CHANGE_CSRF_IGNORE, // Only public routes
            DEBUG_ROUTES             // Only for development
    );

    // ========== UTILITY METHODS ==========

    /**
     * Concatenates multiple String arrays into a single array.
     */
    private static String[] concatenate(String[]... arrays) {
        int totalLength = 0;
        for (String[] array : arrays) {
            totalLength += array.length;
        }

        String[] result = new String[totalLength];
        int currentIndex = 0;

        for (String[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }

        return result;
    }

    // Private constructor to prevent instantiation
    private SecurityConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}