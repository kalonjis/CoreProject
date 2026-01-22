package be.steby.CoreProject.il.routes;

import be.steby.CoreProject.il.routes.password.PasswordRoutes;
import be.steby.CoreProject.il.routes.auth.AuthRoutes;
import be.steby.CoreProject.il.routes.auth.TwoFactorRoutes;
import be.steby.CoreProject.il.routes.account.AccountRoutes;
import be.steby.CoreProject.il.routes.device.DeviceRoutes;
import be.steby.CoreProject.il.routes.address.AddressRoutes;
import be.steby.CoreProject.il.routes.emailchange.EmailChangeRoutes;
import be.steby.CoreProject.il.routes.admin.AdminRoutes;
import be.steby.CoreProject.il.routes.actuator.ActuatorRoutes;
// TODO: Import OAuth2Routes if needed:
// import be.steby.CoreProject.il.routes.auth.OAuth2Routes;
// import be.steby.CoreProject.il.routes.account.AccountRoutes;
// import be.steby.CoreProject.il.routes.device.DeviceRoutes;
// import be.steby.CoreProject.il.routes.address.AddressRoutes;
// import be.steby.CoreProject.il.routes.admin.*;
// import be.steby.CoreProject.il.routes.actuator.ActuatorRoutes;

import static be.steby.CoreProject.il.routes.SecurityRoutes.concatenate;

// TODO MIGRATION: Remove these imports after full migration complete
//import static be.steby.CoreProject.il.utils.SecurityConstants.SWAGGER_ROUTES;
//import static be.steby.CoreProject.il.utils.SecurityConstants.DEBUG_ROUTES;

/**
 * Central aggregator for all security routes configuration.
 * 
 * <p>This class collects route definitions from all domain-specific security
 * configurations and provides unified arrays for Spring Security configuration.
 * It acts as the single source of truth for SecurityConfig.
 * 
 * <p><b>Architecture:</b>
 * <pre>
 * Domain Routes (IL/routes)     →     Aggregator     →     SecurityConfig
 *     └─ auth/
 *         ├─ AuthRoutes                 └─ PUBLIC_ROUTES         └─ permitAll()
 *         ├─ OAuth2Routes               └─ AUTHENTICATED_ROUTES  └─ authenticated()
 *         └─ TwoFactorRoutes            └─ ADMIN_ROUTES          └─ hasRole(ADMIN)
 *     └─ account/                       └─ CSRF_IGNORE           └─ csrf().ignore()
 *         └─ AccountRoutes
 *     └─ password/
 *         └─ PasswordRoutes
 *     └─ device/
 *         └─ DeviceRoutes
 *     └─ etc...
 * </pre>
 * 
 * <p><b>CRITICAL SECURITY PRINCIPLES:</b>
 * <ul>
 *   <li><b>HttpOnly Cookies:</b> This application uses HttpOnly cookies for authentication
 *       (access token + refresh token stored in secure cookies)</li>
 *   <li><b>CSRF Protection:</b> MUST be active for ALL authenticated routes</li>
 *   <li><b>CSRF_IGNORE:</b> Should ONLY include:
 *       <ul>
 *         <li>Public routes that don't require authentication</li>
 *         <li>Routes using tokens from request body/URL (not cookies)</li>
 *       </ul>
 *   </li>
 *   <li><b>NEVER BYPASS CSRF for:</b>
 *       <ul>
 *         <li>Routes using cookie-based authentication</li>
 *         <li>State-changing operations (POST, PUT, DELETE, PATCH)</li>
 *         <li>Admin routes (always sensitive)</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * <p><b>Migration Status:</b>
 * <ul>
 *   <li>✅ Password domain - Complete (il/routes/password/PasswordRoutes)</li>
 *   <li>✅ Auth domain - Complete (il/routes/auth/AuthRoutes, TwoFactorRoutes)</li>
 *   <li>✅ Account domain - Complete (il/routes/account/AccountRoutes)</li>
 *   <li>✅ Device domain - Complete (il/routes/device/DeviceRoutes)</li>
 *   <li>✅ Address domain - Complete (il/routes/address/AddressRoutes)</li>
 *   <li>✅ Email Change domain - Complete (il/routes/emailchange/EmailChangeRoutes)</li>
 *   <li>✅ Admin domain - Complete (il/routes/admin/AdminRoutes)</li>
 *   <li>✅ Actuator domain - Complete (il/routes/actuator/ActuatorRoutes)</li>
 *   <li>⏳ Swagger/Debug routes - Still using old SecurityConstants (TODO: migrate)</li>
 *   <li>⏳ OAuth2 - Optional (il/routes/auth/OAuth2Routes) - if needed</li>
 * </ul>
 *   <li>⏳ Device domain - Pending (il/routes/device/DeviceRoutes)</li>
 *   <li>⏳ Address domain - Pending (il/routes/address/AddressRoutes)</li>
 *   <li>⏳ Email Change domain - Pending (il/routes/emailchange/EmailChangeRoutes)</li>
 *   <li>⏳ Admin domain - Pending (il/routes/admin/AdminUserRoutes, etc.)</li>
 *   <li>⏳ Actuator domain - Pending (il/routes/actuator/ActuatorRoutes)</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 * @see be.steby.CoreProject.il.configs.SecurityConfig
 */
public final class SecurityRoutesAggregator {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * All routes accessible without authentication.
     * 
     * <p>These routes can be accessed by anyone without providing credentials.
     * They typically include login, registration, password reset, and public APIs.
     * 
     * <p><b>Usage in SecurityConfig:</b>
     * <pre>
     * http.authorizeHttpRequests(auth -> auth
     *     .requestMatchers(PUBLIC_ROUTES).permitAll()
     * );
     * </pre>
     * 
     * <p><b>Includes:</b>
     * <ul>
     *   <li>Authentication flows (login, 2FA)</li>
     *   <li>Account registration and activation</li>
     *   <li>Password reset flows</li>
     *   <li>Device confirmation links</li>
     *   <li>Public actuator endpoints (health checks)</li>
     * </ul>
     */
    public static final String[] PUBLIC_ROUTES = concatenate(
        // ✅ Migrated domains - All complete!
        PasswordRoutes.PUBLIC,
        AuthRoutes.PUBLIC,
        TwoFactorRoutes.PUBLIC,
        AccountRoutes.PUBLIC,
        DeviceRoutes.PUBLIC,
        AddressRoutes.PUBLIC,
        EmailChangeRoutes.PUBLIC,
        ActuatorRoutes.PUBLIC
        
        // ⏳ TODO: Migrate these to dedicated route classes
        //SWAGGER_ROUTES,              // TODO: Create SwaggerRoutes.java
        //DEBUG_ROUTES                 // TODO: Create DebugRoutes.java or remove in prod
    );
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Routes requiring authentication but no specific role.
     * 
     * <p>These routes require a valid user session (access token in HttpOnly cookie)
     * but don't require admin privileges. They have CSRF protection enabled.
     * 
     * <p><b>Usage in SecurityConfig:</b>
     * <pre>
     * http.authorizeHttpRequests(auth -> auth
     *     .requestMatchers(AUTHENTICATED_ROUTES).authenticated()
     * );
     * </pre>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ CSRF protection is ACTIVE (not in CSRF_IGNORE)</li>
     *   <li>✅ Requires valid access token in HttpOnly cookie</li>
     *   <li>✅ Session-based authentication with device tracking</li>
     * </ul>
     * 
     * <p><b>Includes:</b>
     * <ul>
     *   <li>User profile management</li>
     *   <li>Password change</li>
     *   <li>2FA settings</li>
     *   <li>Device management</li>
     *   <li>Address management</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED_ROUTES = concatenate(
        // ✅ Migrated domains - All complete!
        PasswordRoutes.AUTHENTICATED,
        AuthRoutes.AUTHENTICATED,
        TwoFactorRoutes.AUTHENTICATED,
        AccountRoutes.AUTHENTICATED,
        DeviceRoutes.AUTHENTICATED,
        AddressRoutes.AUTHENTICATED,
        EmailChangeRoutes.AUTHENTICATED,
        ActuatorRoutes.AUTHENTICATED
    );
    
    // ========== ADMIN ROUTES ==========
    
    /**
     * Routes requiring ADMIN or SUPER_ADMIN authority.
     * 
     * <p>These routes expose sensitive administrative operations and user data.
     * All admin routes MUST have CSRF protection (never in CSRF_IGNORE).
     * 
     * <p><b>Usage in SecurityConfig:</b>
     * <pre>
     * http.authorizeHttpRequests(auth -> auth
     *     .requestMatchers(ADMIN_ROUTES).hasAnyAuthority("ADMIN", "SUPER_ADMIN")
     * );
     * </pre>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ CSRF protection is ALWAYS ACTIVE (never bypassed)</li>
     *   <li>✅ Requires ADMIN or SUPER_ADMIN authority</li>
     *   <li>✅ All operations are logged for audit</li>
     *   <li>❌ NEVER in CSRF_IGNORE (even in development)</li>
     * </ul>
     * 
     * <p><b>Includes:</b>
     * <ul>
     *   <li>User management (create, activate, deactivate, roles)</li>
     *   <li>Cache management (invalidation, cleanup)</li>
     *   <li>Security logs access</li>
     *   <li>Device management (disconnect, trust levels)</li>
     *   <li>Sensitive actuator endpoints</li>
     * </ul>
     */
    public static final String[] ADMIN_ROUTES = concatenate(
        // ✅ Migrated domains - All complete!
        AdminRoutes.ADMIN,
        ActuatorRoutes.ADMIN
    );
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>⚠️ CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ ONLY include public routes that use tokens from request body/URL</li>
     *   <li>❌ NEVER include routes that use HttpOnly cookies for authentication</li>
     *   <li>❌ NEVER include AUTHENTICATED_ROUTES</li>
     *   <li>❌ NEVER include ADMIN_ROUTES</li>
     * </ul>
     * 
     * <p><b>Why These Routes Can Safely Ignore CSRF:</b>
     * <ul>
     *   <li>No cookies involved - authentication via tokens in request body/URL</li>
     *   <li>One-time use tokens with short expiration</li>
     *   <li>Cryptographically secure random tokens</li>
     *   <li>Rate limiting prevents abuse</li>
     * </ul>
     * 
     * <p><b>Example Safe Routes:</b>
     * <ul>
     *   <li>/api/auth/login - credentials in request body, no prior session</li>
     *   <li>/api/password/reset - token from email link</li>
     *   <li>/api/account/activate/* - activation token from email</li>
     * </ul>
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify no authenticated routes are in this list</li>
     *   <li>✅ Verify no admin routes are in this list</li>
     *   <li>✅ Remove all DEBUG_ROUTES</li>
     *   <li>✅ Remove temporary development exceptions</li>
     *   <li>✅ Ensure all cookie-based routes have CSRF protection</li>
     * </ol>
     * 
     * <p><b>Usage in SecurityConfig:</b>
     * <pre>
     * http.csrf(csrf -> csrf
     *     .ignoringRequestMatchers(CSRF_IGNORE)
     * );
     * </pre>
     */
    public static final String[] CSRF_IGNORE = concatenate(
        // ✅ Migrated domains - All complete!
        PasswordRoutes.CSRF_IGNORE,
        AuthRoutes.CSRF_IGNORE,
        TwoFactorRoutes.CSRF_IGNORE,
        AccountRoutes.CSRF_IGNORE,
        DeviceRoutes.CSRF_IGNORE,
        AddressRoutes.CSRF_IGNORE,          // Empty - correct!
        EmailChangeRoutes.CSRF_IGNORE,
        AdminRoutes.CSRF_IGNORE,            // Empty - correct!
        ActuatorRoutes.CSRF_IGNORE
        
        // ⏳ TODO: Migrate these to dedicated route classes
        //SWAGGER_ROUTES,                     // TODO: Move to SwaggerRoutes.CSRF_IGNORE
        //DEBUG_ROUTES                        // ⚠️ TODO PRODUCTION: REMOVE BEFORE DEPLOYMENT
        
        // ✅ REMOVED - These were CRITICAL SECURITY RISKS:
        // ADMIN_DOMAIN_ROUTES           ❌ Removed - Admin routes NEVER bypass CSRF
        // ADDRESS_AUTHENTICATED_ROUTES  ❌ Removed - Address routes NEVER bypass CSRF
    );
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private SecurityRoutesAggregator() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
}