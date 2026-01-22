package be.steby.CoreProject.il.routes.password;

/**
 * Security routes configuration for Password Management domain.
 * 
 * <p>This class defines all security-related route configurations for the password
 * management bounded context, following DDD principles. It includes public routes
 * (password reset flows), authenticated routes (password change), and CSRF configuration.
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>Password reset request and confirmation (public, token-based)</li>
 *   <li>Password change for authenticated users (cookie-based)</li>
 *   <li>Password definition for newly created accounts</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Public routes use tokens from email/URL - CSRF can be ignored</li>
 *   <li>Authenticated routes use HttpOnly cookies - CSRF protection REQUIRED</li>
 *   <li>Password change is highly sensitive - always requires authentication + CSRF</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class PasswordRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * Public password routes accessible without authentication.
     * 
     * <p>These routes handle password reset flows initiated via email.
     * They use tokens passed in the request body or URL parameters,
     * not cookies, so they're safe to exclude from CSRF protection.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/password/forgot - Request password reset email</li>
     *   <li>POST /api/password/reset - Reset password with token</li>
     *   <li>POST /api/password/verify-code - Verify reset code validity</li>
     *   <li>POST /api/password/reset-with-permission - Admin-initiated reset</li>
     *   <li>POST /api/password/reset/resend - Resend reset email</li>
     * </ul>
     * 
     * <p><b>Security:</b> All use one-time tokens from email, rate-limited.
     */
    public static final String[] PUBLIC = {
        "/api/password/forgot",
        "/api/password/reset",
        "/api/password/verify-code",
        "/api/password/reset-with-permission",
        "/api/password/reset/resend"
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated password routes requiring user login.
     * 
     * <p>These routes use HttpOnly cookies for authentication and MUST have
     * CSRF protection enabled (NOT in CSRF_IGNORE).
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/password/change - Change password (requires current password)</li>
     *   <li>POST /api/password/define - Set password for accounts without one (OAuth users)</li>
     * </ul>
     * 
     * <p><b>Security:</b> 
     * <ul>
     *   <li>Requires valid authentication (access token in HttpOnly cookie)</li>
     *   <li>CSRF protection is ACTIVE (not in CSRF_IGNORE)</li>
     *   <li>/change requires current password validation</li>
     *   <li>/define only for users without existing password</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
        "/api/password/change",
        "/api/password/define"
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ ONLY public routes using tokens from request body/URL</li>
     *   <li>❌ NEVER include authenticated routes (they use cookies)</li>
     *   <li>❌ NEVER include /change or /define (highly sensitive operations)</li>
     * </ul>
     * 
     * <p><b>Why these are safe to ignore CSRF:</b>
     * <ul>
     *   <li>No cookies involved - tokens come from email links</li>
     *   <li>One-time use tokens with short expiration</li>
     *   <li>Rate limiting prevents brute force</li>
     *   <li>Tokens are cryptographically secure random strings</li>
     * </ul>
     * 
     * <p><b>Production Configuration:</b>
     * In production, remove /change and /define from this list if present.
     * They should NEVER bypass CSRF protection.
     * 
     * <p><b>Current Status:</b>
     * ⚠️ TODO: Remove /change and /define before production deployment.
     * These are temporarily here for development/testing only.
     */
    public static final String[] CSRF_IGNORE = {
        "/api/password/forgot",
        "/api/password/reset",
        "/api/password/reset/resend",
        "/api/password/verify-code",
        "/api/password/reset-with-permission"
        // TODO PRODUCTION: Verify that /change and /define are NOT here
        // Uncomment the lines below ONLY for development/testing:
        // "/api/password/change",    // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/password/define"     // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private PasswordRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}