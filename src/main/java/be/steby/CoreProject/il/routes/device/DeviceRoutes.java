package be.steby.CoreProject.il.routes.device;

/**
 * Security routes configuration for Device Management domain.
 * 
 * <p>This class defines security routes for device tracking, trust management,
 * and session control. The application tracks user devices and allows users
 * to manage trusted devices.
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>Device registration and fingerprinting</li>
 *   <li>Device confirmation via email (new device detected)</li>
 *   <li>Device trust level management (UNTRUSTED, BASIC, TRUSTED, HIGHLY_TRUSTED)</li>
 *   <li>Session management per device</li>
 *   <li>Device disconnection and revocation</li>
 * </ul>
 * 
 * <p><b>Device Security Flow:</b>
 * <ol>
 *   <li>User logs in from new device → Device fingerprinted</li>
 *   <li>If new device detected → Email sent with confirmation link</li>
 *   <li>User clicks confirm/reject in email → Device confirmed or blocked</li>
 *   <li>User can manage devices in settings (disconnect, change trust level)</li>
 *   <li>Critical operations may require TRUSTED or HIGHLY_TRUSTED device</li>
 * </ol>
 * 
 * <p><b>Trust Levels:</b>
 * <ul>
 *   <li><b>UNTRUSTED:</b> New unconfirmed device (limited access)</li>
 *   <li><b>BASIC:</b> Email-confirmed device (normal access)</li>
 *   <li><b>TRUSTED:</b> User-elevated trust (sensitive operations)</li>
 *   <li><b>HIGHLY_TRUSTED:</b> Maximum trust (financial operations, admin)</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Email confirmation uses one-time tokens - CSRF can be ignored</li>
 *   <li>Device management requires authentication - CSRF protection needed</li>
 *   <li>Disconnecting devices invalidates associated sessions</li>
 *   <li>Device fingerprinting helps detect suspicious login patterns</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class DeviceRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * Public device routes for email-based confirmation.
     * 
     * <p>These routes are accessed via email links when a new device is detected.
     * They use one-time tokens, not cookies, so CSRF can be safely ignored.
     * 
     * <p><b>Email-Based Confirmation:</b>
     * <ul>
     *   <li>GET /api/device/confirm - Confirm device via email token (trust level → BASIC)</li>
     *   <li>GET /api/device/reject - Reject/block device via email token</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>One-time tokens sent to user's verified email</li>
     *   <li>Tokens expire after 24-48 hours</li>
     *   <li>No cookies involved - safe from CSRF</li>
     *   <li>Rejecting a device invalidates its sessions immediately</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>User logs in from new laptop → Email sent → Clicks confirm link</li>
     *   <li>Suspicious login detected → Email sent → User clicks reject link</li>
     * </ul>
     */
    public static final String[] PUBLIC = {
        "/api/device/confirm",      // Confirm device via email token
        "/api/device/reject"        // Reject device via email token
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated device routes for device management.
     * 
     * <p>These routes allow authenticated users to view and manage their devices.
     * All use HttpOnly cookies for authentication and MUST have CSRF protection.
     * 
     * <p><b>Device Information:</b>
     * <ul>
     *   <li>GET /api/device/current - Get information about current device</li>
     *   <li>GET /api/device/session - Get current session info (device + login details)</li>
     *   <li>GET /api/device/my-devices - List all user's registered devices</li>
     *   <li>GET /api/device/* - Get specific device details by ID</li>
     * </ul>
     * 
     * <p><b>Device Management:</b>
     * <ul>
     *   <li>POST   /api/device/request-confirmation - Manually request device confirmation email</li>
     *   <li>POST   /api/device/disconnect-all-others - Disconnect all other devices (keep current)</li>
     *   <li>POST   /api/device/disconnect/* - Disconnect specific device by ID</li>
     *   <li>PATCH  /api/device/trust-level/* - Update device trust level (BASIC → TRUSTED, etc.)</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ All use HttpOnly cookies - CSRF protection REQUIRED</li>
     *   <li>✅ Users can only manage their own devices</li>
     *   <li>✅ Disconnecting device immediately invalidates refresh tokens</li>
     *   <li>✅ Trust level changes may require password confirmation</li>
     *   <li>✅ disconnect-all-others security feature for account compromise</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>User views devices in settings → /my-devices</li>
     *   <li>Lost phone → /disconnect/{phoneDeviceId}</li>
     *   <li>Suspicious activity → /disconnect-all-others</li>
     *   <li>Elevate laptop trust → /trust-level/{laptopId} → TRUSTED</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
        // Device Information
        "/api/device/current",
        "/api/device/session",
        "/api/device/my-devices",
        "/api/device/*",                      // GET device by ID
        
        // Device Management
        "/api/device/request-confirmation",
        "/api/device/disconnect-all-others",
        "/api/device/disconnect/*",           // Disconnect by ID
        "/api/device/trust-level/*"           // Update trust level by ID
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ ONLY email-based confirmation routes (confirm, reject)</li>
     *   <li>❌ NEVER device management routes (they use cookies)</li>
     *   <li>❌ NEVER disconnect operations (critical security operations)</li>
     * </ul>
     * 
     * <p><b>Safe Routes (Email Tokens):</b>
     * <ul>
     *   <li>/device/confirm: One-time token from email → Safe</li>
     *   <li>/device/reject: One-time token from email → Safe</li>
     * </ul>
     * 
     * <p><b>⚠️ NEVER Bypass CSRF For:</b>
     * <ul>
     *   <li>All /device management routes (use cookies)</li>
     *   <li>/disconnect/* (critical security operation)</li>
     *   <li>/disconnect-all-others (critical security operation)</li>
     *   <li>/trust-level/* (modifies security settings)</li>
     *   <li>/current, /session, /my-devices (use cookies)</li>
     * </ul>
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify ONLY /confirm and /reject are in CSRF_IGNORE</li>
     *   <li>✅ Ensure ALL authenticated device routes have CSRF protection</li>
     *   <li>✅ Remove temporary development exceptions</li>
     *   <li>✅ Test disconnect operations with CSRF enabled</li>
     * </ol>
     * 
     * <p><b>⚠️ Old Configuration Issue:</b>
     * The old SecurityConstants has many device routes in CSRF_IGNORE for development.
     * This is extremely dangerous as it allows CSRF attacks on device management:
     * <ul>
     *   <li>Attacker could disconnect user's devices</li>
     *   <li>Attacker could change device trust levels</li>
     *   <li>Attacker could request fake confirmation emails</li>
     * </ul>
     * 
     * <p><b>TODO PRODUCTION:</b>
     * Remove ALL these from CSRF_IGNORE before production:
     * <ul>
     *   <li>/api/device/current ← Uses cookies, needs CSRF</li>
     *   <li>/api/device/session ← Uses cookies, needs CSRF</li>
     *   <li>/api/device/my-devices ← Uses cookies, needs CSRF</li>
     *   <li>/api/device/request-confirmation ← Uses cookies, needs CSRF</li>
     *   <li>/api/device/disconnect-all-others ← CRITICAL - needs CSRF</li>
     *   <li>/api/device/trust-level/* ← Security setting - needs CSRF</li>
     *   <li>/api/device/disconnect/* ← CRITICAL - needs CSRF</li>
     *   <li>/api/device/* ← Uses cookies, needs CSRF</li>
     * </ul>
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ Safe - Email tokens, no cookies
        "/api/device/confirm",
        "/api/device/reject"
        
        // TODO PRODUCTION: Ensure ALL these are NOT here (they're in old SecurityConstants):
        // "/api/device/current",                    // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/device/session",                    // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/device/my-devices",                 // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/device/request-confirmation",       // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/device/disconnect-all-others",      // ⚠️ CRITICAL - MUST HAVE CSRF IN PROD
        // "/api/device/trust-level/*",              // ⚠️ SECURITY RISK - REMOVE IN PROD
        // "/api/device/disconnect/*",               // ⚠️ CRITICAL - MUST HAVE CSRF IN PROD
        // "/api/device/*"                           // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private DeviceRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}