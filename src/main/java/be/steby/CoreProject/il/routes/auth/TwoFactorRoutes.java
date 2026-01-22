package be.steby.CoreProject.il.routes.auth;

/**
 * Security routes configuration for Two-Factor Authentication (2FA).
 * 
 * <p>This class defines all security routes related to 2FA setup, verification,
 * and management. It supports multiple 2FA methods: Email, SMS, TOTP, and Backup Codes.
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>2FA verification during login flow</li>
 *   <li>2FA method setup and configuration (Email, SMS, TOTP, Backup Codes)</li>
 *   <li>2FA method enable/disable operations</li>
 *   <li>2FA settings and available methods retrieval</li>
 * </ul>
 * 
 * <p><b>2FA Flow:</b>
 * <ol>
 *   <li>User logs in with credentials → redirected to 2FA if enabled</li>
 *   <li>User chooses 2FA method → POST /2fa/choose-method</li>
 *   <li>User enters 2FA code → POST /verify-2fa</li>
 *   <li>Code verified → session established</li>
 * </ol>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Login-phase 2FA (verify, resend) uses temporary tokens - CSRF can be ignored</li>
 *   <li>Settings-phase 2FA (setup, enable, disable) uses cookies - REQUIRES CSRF</li>
 *   <li>TOTP setup generates QR codes for authenticator apps</li>
 *   <li>Backup codes are one-time use emergency access codes</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 * @see AuthRoutes
 */
public final class TwoFactorRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * Public 2FA routes used during the login flow.
     * 
     * <p>These routes are part of the login process, after initial credentials
     * are verified but before the session is fully established. They use
     * temporary tokens, not cookies, so CSRF can be safely ignored.
     * 
     * <p><b>Login Flow Endpoints:</b>
     * <ul>
     *   <li>POST /api/auth/verify-2fa - Verify 2FA code during login</li>
     *   <li>POST /api/auth/resend-2fa-code - Resend 2FA code (Email/SMS)</li>
     *   <li>GET  /api/auth/2fa-status - Check if 2FA is required for login</li>
     *   <li>POST /api/auth/2fa/choose-method - Select which 2FA method to use</li>
     *   <li>GET  /api/auth/2fa/login/methods - Get available 2FA methods for login</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>Used during login - no established session yet</li>
     *   <li>Temporary login tokens in request body, not cookies</li>
     *   <li>Rate limited to prevent brute force attacks</li>
     *   <li>Codes expire after short time (5-10 minutes)</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>User enters username/password → redirected to /verify-2fa</li>
     *   <li>Email code didn't arrive → /resend-2fa-code</li>
     *   <li>User has multiple 2FA methods → /choose-method to select one</li>
     * </ul>
     */
    public static final String[] PUBLIC = {
        "/api/auth/verify-2fa",
        "/api/auth/resend-2fa-code",
        "/api/auth/2fa-status",
        "/api/auth/2fa/choose-method",
        "/api/auth/2fa/login/methods"
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated 2FA routes for setup and management.
     * 
     * <p>These routes allow authenticated users to configure their 2FA settings.
     * All use HttpOnly cookies for authentication and MUST have CSRF protection.
     * 
     * <p><b>General Settings:</b>
     * <ul>
     *   <li>GET /api/auth/2fa/settings - Get current 2FA configuration</li>
     *   <li>GET /api/auth/2fa/available-methods - Get all available 2FA methods</li>
     * </ul>
     * 
     * <p><b>Email 2FA:</b>
     * <ul>
     *   <li>POST /api/auth/2fa/email/setup/initiate - Start email 2FA setup</li>
     *   <li>POST /api/auth/2fa/email/setup/verify - Verify email 2FA code during setup</li>
     *   <li>POST /api/auth/2fa/email/enable - Enable email 2FA</li>
     *   <li>POST /api/auth/2fa/email/disable - Disable email 2FA</li>
     * </ul>
     * 
     * <p><b>SMS 2FA:</b>
     * <ul>
     *   <li>POST /api/auth/2fa/sms/enable - Enable SMS 2FA (requires verified phone)</li>
     *   <li>POST /api/auth/2fa/sms/disable - Disable SMS 2FA</li>
     *   <li>POST /api/profile/SMS/request-verification - Request SMS verification code (TODO: move to profile)</li>
     *   <li>POST /api/profile/SMS/verify - Verify SMS code (TODO: move to profile)</li>
     * </ul>
     * 
     * <p><b>TOTP (Authenticator App) 2FA:</b>
     * <ul>
     *   <li>POST /api/auth/2fa/totp/setup/initiate - Generate QR code for authenticator app</li>
     *   <li>POST /api/auth/2fa/totp/setup/verify - Verify TOTP code to complete setup</li>
     *   <li>POST /api/auth/2fa/totp/enable - Enable TOTP 2FA</li>
     *   <li>POST /api/auth/2fa/totp/disable - Disable TOTP 2FA</li>
     *   <li>POST /api/auth/2fa/totp/test-code - Test TOTP code validity</li>
     * </ul>
     * 
     * <p><b>Backup Codes 2FA:</b>
     * <ul>
     *   <li>POST /api/auth/2fa/backup-codes/enable - Generate backup codes</li>
     *   <li>POST /api/auth/2fa/backup-codes/disable - Disable backup codes</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ All use HttpOnly cookies - CSRF protection REQUIRED</li>
     *   <li>✅ Enable/disable operations require password confirmation</li>
     *   <li>✅ Setup requires code verification before activation</li>
     *   <li>✅ Backup codes generated once, shown once, then hashed</li>
     * </ul>
     * 
     * <p><b>⚠️ TODO:</b> Move SMS verification routes to profile domain:
     * <ul>
     *   <li>/api/profile/SMS/request-verification → ProfileRoutes</li>
     *   <li>/api/profile/SMS/verify → ProfileRoutes</li>
     * </ul>
     */
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
        
        // TOTP (Authenticator App) 2FA
        "/api/auth/2fa/totp/enable",
        "/api/auth/2fa/totp/disable",
        "/api/auth/2fa/totp/test-code",
        "/api/auth/2fa/totp/setup/initiate",
        "/api/auth/2fa/totp/setup/verify",
        
        // Backup Codes 2FA
        "/api/auth/2fa/backup-codes/enable",
        "/api/auth/2fa/backup-codes/disable",
        
        // TODO: Move these to ProfileRoutes (SMS verification)
        "/api/profile/SMS/request-verification",
        "/api/profile/SMS/verify"
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ ONLY login-phase 2FA routes (no cookies, use temporary tokens)</li>
     *   <li>❌ NEVER settings-phase 2FA routes (they use cookies)</li>
     *   <li>❌ NEVER enable/disable operations (highly sensitive)</li>
     * </ul>
     * 
     * <p><b>Safe Routes (Login Phase):</b>
     * <ul>
     *   <li>/verify-2fa: Temporary login token, no cookies → Safe</li>
     *   <li>/resend-2fa-code: Temporary login token → Safe</li>
     *   <li>/2fa-status: Public check, no state change → Safe</li>
     *   <li>/choose-method: Login phase selection → Safe</li>
     *   <li>/login/methods: Public information → Safe</li>
     * </ul>
     * 
     * <p><b>⚠️ NEVER Bypass CSRF For:</b>
     * <ul>
     *   <li>All /setup/* routes (use cookies)</li>
     *   <li>All /enable routes (critical security operation)</li>
     *   <li>All /disable routes (critical security operation)</li>
     *   <li>/settings (uses cookies)</li>
     *   <li>/available-methods when authenticated (uses cookies)</li>
     * </ul>
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify ONLY login-phase routes are in CSRF_IGNORE</li>
     *   <li>✅ Ensure NO setup/enable/disable routes bypass CSRF</li>
     *   <li>✅ Remove temporary development exceptions</li>
     *   <li>✅ Confirm all authenticated 2FA routes have CSRF protection</li>
     * </ol>
     * 
     * <p><b>Current Temporary Exceptions (REMOVE IN PROD):</b>
     * The old SecurityConstants has these in CSRF_IGNORE for development.
     * They MUST be removed in production as they use cookies:
     * <ul>
     *   <li>All /setup/* routes</li>
     *   <li>All /enable routes</li>
     *   <li>All /disable routes</li>
     *   <li>/settings, /available-methods, /test-code</li>
     * </ul>
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ Safe - Login phase, temporary tokens, no cookies
        "/api/auth/verify-2fa",
        "/api/auth/resend-2fa-code",
        "/api/auth/2fa-status",
        "/api/auth/2fa/choose-method",
        "/api/auth/2fa/login/methods"
        
        // Also in old SecurityConstants but should be safe (public info):
        // "/api/auth/2fa/methods"  // Alias for /login/methods?
        
        // TODO PRODUCTION: Ensure ALL these are NOT here:
        // "/api/auth/2fa/email/setup/initiate",     // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/email/setup/verify",       // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/enable-email",             // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/email/enable",             // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/email/disable",            // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/sms/enable",               // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/sms/disable",              // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/totp/enable",              // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/totp/disable",             // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/totp/test-code",           // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/totp/setup/initiate",      // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/totp/setup/verify",        // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/backup-codes/enable",      // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/backup-codes/disable",     // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/2fa/available-methods",        // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/profile/SMS/request-verification",  // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/profile/SMS/verify"                 // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private TwoFactorRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}