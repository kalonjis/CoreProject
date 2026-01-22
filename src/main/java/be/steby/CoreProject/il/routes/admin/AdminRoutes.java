package be.steby.CoreProject.il.routes.admin;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

/**
 * Security routes configuration for Admin Management domain.
 * 
 * <p>This class defines all administrative routes requiring ADMIN or SUPER_ADMIN
 * authority. Admin routes expose sensitive operations and user data, and ALL of
 * them MUST have CSRF protection enabled.
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>User management (create, activate, deactivate, roles, GDPR)</li>
 *   <li>Address management (admin view/modify user addresses)</li>
 *   <li>Device management (admin control over user devices)</li>
 *   <li>Cache management (invalidation, cleanup, maintenance)</li>
 *   <li>Security logs and audit trails</li>
 *   <li>Password reset operations (admin-initiated)</li>
 *   <li>Circuit breaker manual control</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>ALL routes require ADMIN or SUPER_ADMIN role</li>
 *   <li>ALL routes use cookies - CSRF protection MANDATORY</li>
 *   <li>ALL operations are logged for audit</li>
 *   <li>Sensitive data access restricted to authorized admins</li>
 *   <li>State-changing operations require explicit authorization</li>
 * </ul>
 * 
 * <p><b>⚠️ CRITICAL SECURITY WARNING:</b>
 * Admin routes are NEVER in CSRF_IGNORE, even in development.
 * CSRF attacks on admin routes could:
 * <ul>
 *   <li>Delete user accounts</li>
 *   <li>Change user roles (escalate privileges)</li>
 *   <li>Access sensitive user data</li>
 *   <li>Manipulate cache (cause data inconsistency)</li>
 *   <li>Trigger GDPR deletions</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class AdminRoutes {
    
    // ========== NO PUBLIC ROUTES ==========
    
    /**
     * No public routes for admin operations.
     * 
     * <p>All admin operations require ADMIN or SUPER_ADMIN authority.
     */
    public static final String[] PUBLIC = {
        // No public routes - all admin operations require ADMIN role
    };
    
    // ========== NO AUTHENTICATED ROUTES (NON-ADMIN) ==========
    
    /**
     * No regular authenticated routes for admin operations.
     * 
     * <p>Regular authenticated users cannot access admin operations.
     * All admin operations are in ADMIN_ROUTES.
     */
    public static final String[] AUTHENTICATED = {
        // No regular authenticated routes - all operations require ADMIN role
    };
    
    // ========== ADMIN ROUTES ==========
    
    /**
     * User management routes - ADMIN only.
     * 
     * <p>Endpoints for managing user accounts, roles, and lifecycle.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>GET    /api/admin/users - Get all users (deprecated, use /all)</li>
     *   <li>GET    /api/admin/users/all - Get paginated users list</li>
     *   <li>POST   /api/admin/users/create - Create new user account</li>
     *   <li>GET    /api/admin/users/stats - Get admin statistics</li>
     *   <li>PATCH  /api/admin/users/activate/** - Activate user account</li>
     *   <li>PATCH  /api/admin/users/deactivate/** - Deactivate user account</li>
     *   <li>POST   /api/admin/users/force-reset-password/** - Force password reset</li>
     *   <li>PATCH  /api/admin/users/grant-role/** - Grant role to user</li>
     *   <li>PATCH  /api/admin/users/revoke-role/** - Revoke role from user</li>
     *   <li>DELETE /api/admin/users/gdpr-deletion/** - GDPR compliant user deletion</li>
     *   <li>GET    /api/admin/users/deactivation-categories - Get deactivation reason categories</li>
     * </ul>
     */
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
    
    /**
     * User address management routes - ADMIN only.
     * 
     * <p>Endpoints for admins to manage user addresses.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>GET    /api/admin/users/* /addresses - Get all addresses for a user</li>
     *   <li>POST   /api/admin/users/* /addresses - Create address for user</li>
     *   <li>PATCH  /api/admin/users/* /addresses/* /metadata - Update address metadata</li>
     *   <li>DELETE /api/admin/users/* /addresses/** - Delete user address link</li>
     *   <li>DELETE /api/admin/users/* /addresses/* /permanent - Permanently delete address</li>
     * </ul>
     */
    private static final String[] USER_ADDRESS_MANAGEMENT = {
        "/api/admin/users/*/addresses",
        "/api/admin/users/*/addresses/*/metadata",
        "/api/admin/users/*/addresses/**",
        "/api/admin/users/*/addresses/*/permanent"
    };
    
    /**
     * Shared address management routes - ADMIN only.
     * 
     * <p>Endpoints for managing shared addresses (addresses used by multiple users).
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>GET    /api/admin/addresses - Get all shared addresses</li>
     *   <li>GET    /api/admin/addresses/* - Get address details with linked users</li>
     *   <li>DELETE /api/admin/addresses/* - Delete orphan address</li>
     *   <li>GET    /api/admin/addresses/* /users - Get all users linked to address</li>
     * </ul>
     */
    private static final String[] ADDRESS_MANAGEMENT = {
        "/api/admin/addresses",
        "/api/admin/addresses/*",
        "/api/admin/addresses/*/users"
    };
    
    /**
     * Device management routes - ADMIN only.
     * 
     * <p>Endpoints for admins to manage user devices.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>GET /api/admin/device/list/user/** - Get all devices for a user</li>
     *   <li>GET /api/admin/device/list/** - Get device list (generic)</li>
     * </ul>
     */
    private static final String[] DEVICE_MANAGEMENT = {
        "/api/admin/device/list/user/**",
        "/api/admin/device/list/**"
    };
    
    /**
     * Password management routes - ADMIN only.
     * 
     * <p>Endpoints for admin-initiated password operations.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/admin/password-reset/** - Force password reset</li>
     *   <li>POST /api/admin/password-reset/send-reset-link/** - Send password reset link</li>
     *   <li>POST /api/admin/password-reset/send-temporary-password/** - Send temporary password</li>
     *   <li>POST /api/admin/password-reset/send-via-alternative-channel/** - Send via SMS/etc</li>
     * </ul>
     */
    private static final String[] PASSWORD_MANAGEMENT = {
        "/api/admin/password-reset/**",
        "/api/admin/password-reset/send-reset-link/**",
        "/api/admin/password-reset/send-temporary-password/**",
        "/api/admin/password-reset/send-via-alternative-channel/**"
    };
    
    /**
     * Device cache management routes - ADMIN only.
     * 
     * <p>Endpoints for managing device security cache.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>GET    /api/admin/cache/device/stats - Get cache statistics</li>
     *   <li>POST   /api/admin/cache/device/cleanup - Cleanup stale cache entries</li>
     *   <li>DELETE /api/admin/cache/device/** - Invalidate specific device cache</li>
     * </ul>
     */
    private static final String[] CACHE_DEVICE_MANAGEMENT = {
        "/api/admin/cache/device/stats",
        "/api/admin/cache/device/cleanup",
        "/api/admin/cache/device/**"
    };
    
    /**
     * User cache management routes - ADMIN only.
     * 
     * <p>Endpoints for managing user data cache.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>GET    /api/admin/cache/user/stats - Get cache statistics</li>
     *   <li>GET    /api/admin/cache/user/health - Get cache health status</li>
     *   <li>POST   /api/admin/cache/user/cleanup - Cleanup stale cache entries</li>
     *   <li>POST   /api/admin/cache/user/maintenance - Run cache maintenance</li>
     *   <li>DELETE /api/admin/cache/user/username/** - Invalidate cache by username</li>
     *   <li>DELETE /api/admin/cache/user/id/** - Invalidate cache by user ID</li>
     *   <li>DELETE /api/admin/cache/user/role/** - Invalidate cache by role</li>
     *   <li>POST   /api/admin/cache/user/security-incident - Handle security incident</li>
     *   <li>GET    /api/admin/cache/user/config - Get cache configuration</li>
     * </ul>
     */
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
    
    /**
     * Security logs and audit routes - ADMIN only.
     * 
     * <p>Endpoints for accessing security logs and audit trails.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>GET /api/security/logs/user/** - Get security logs for a user</li>
     * </ul>
     */
    private static final String[] SECURITY_LOGS = {
        "/api/security/logs/user/**"
    };
    
    /**
     * Circuit breaker management routes - ADMIN only.
     * 
     * <p>Endpoints for manually controlling circuit breakers.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/admin/circuit-breaker/** - Manual circuit breaker operations</li>
     * </ul>
     */
    private static final String[] CIRCUIT_BREAKER_MANAGEMENT = {
        "/api/admin/circuit-breaker/**"
    };
    
    /**
     * All admin routes combined.
     * 
     * <p>This aggregates all admin-only routes from all categories.
     * Used by SecurityRoutesAggregator to configure Spring Security.
     * 
     * <p><b>⚠️ CRITICAL SECURITY:</b>
     * <ul>
     *   <li>✅ ALL routes require ADMIN or SUPER_ADMIN authority</li>
     *   <li>✅ ALL routes have CSRF protection (NEVER in CSRF_IGNORE)</li>
     *   <li>✅ ALL operations are logged for audit</li>
     *   <li>❌ NEVER expose to regular users</li>
     *   <li>❌ NEVER bypass CSRF protection</li>
     * </ul>
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
        CIRCUIT_BREAKER_MANAGEMENT
    );
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>⚠️ ABSOLUTE RULE:</b>
     * <ul>
     *   <li>❌ NO admin routes should EVER bypass CSRF protection</li>
     *   <li>❌ Not in development</li>
     *   <li>❌ Not in testing</li>
     *   <li>❌ Not in staging</li>
     *   <li>❌ Not in production</li>
     *   <li>❌ NEVER, UNDER ANY CIRCUMSTANCES</li>
     * </ul>
     * 
     * <p><b>Why Admin Routes MUST NEVER Bypass CSRF:</b>
     * <ul>
     *   <li>All use cookie-based authentication</li>
     *   <li>All are state-changing operations (create, update, delete)</li>
     *   <li>Expose sensitive user data and system internals</li>
     *   <li>CSRF attack on admin routes could:
     *     <ul>
     *       <li>Delete user accounts (GDPR deletion)</li>
     *       <li>Grant admin privileges to attacker</li>
     *       <li>Access all user data (privacy breach)</li>
     *       <li>Manipulate cache (data corruption)</li>
     *       <li>Force password resets (account takeover)</li>
     *       <li>Disable circuit breakers (DoS vulnerability)</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <p><b>⚠️ EXTREME SECURITY RISK:</b>
     * The old SecurityConstants has ADMIN_DOMAIN_ROUTES in CSRF_IGNORE
     * for "development convenience". This is an EXTREME SECURITY RISK
     * and creates a critical vulnerability even in development environments.
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify NO admin routes are in CSRF_IGNORE</li>
     *   <li>✅ Verify ADMIN_DOMAIN_ROUTES removed from SecurityRoutesAggregator.CSRF_IGNORE</li>
     *   <li>✅ Test all admin operations with CSRF tokens</li>
     *   <li>✅ Ensure admin operations fail without CSRF token</li>
     * </ol>
     * 
     * <p><b>Correct Configuration:</b>
     * This array is and must ALWAYS remain empty. NO EXCEPTIONS.
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ CORRECT - Empty array
        // NO admin routes should EVER bypass CSRF protection
        
        // ❌ NEVER ADD ANY ADMIN ROUTES HERE
        // ❌ Not even for development
        // ❌ Not even for testing
        // ❌ NEVER
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private AdminRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}