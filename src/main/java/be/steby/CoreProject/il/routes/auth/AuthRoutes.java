package be.steby.CoreProject.il.routes.auth;

/**
 * Security routes configuration for core Authentication operations.
 * 
 * <p>This class defines security routes for basic authentication flows including
 * login, logout, session management, and token refresh. It does NOT include
 * 2FA routes (see {@link TwoFactorRoutes}) or OAuth2 routes (see {@link OAuth2Routes}).
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>User login with credentials (username/email + password)</li>
 *   <li>Session management and status checking</li>
 *   <li>Token refresh (access token renewal)</li>
 *   <li>User logout and session termination</li>
 *   <li>Current user information retrieval</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Login uses credentials in request body - safe to ignore CSRF</li>
 *   <li>Logout uses HttpOnly cookies - MUST have CSRF protection</li>
 *   <li>Token refresh uses refresh token cookie - MUST have CSRF protection</li>
 *   <li>Session endpoints use access token cookie - MUST have CSRF protection</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 * @see TwoFactorRoutes
 * @see OAuth2Routes
 */
public final class AuthRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * Public authentication routes accessible without prior authentication.
     * 
     * <p>These routes handle the initial authentication flow before any
     * session or tokens exist. They're public because users need to access
     * them to establish a session.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/auth/login - Traditional login (username/email + password)</li>
     *   <li>POST /api/auth/initiate-login - Start login flow (may redirect to 2FA)</li>
     *   <li>POST /api/auth/refresh-token - Renew access token using refresh token cookie</li>
     * </ul>
     * 
     * <p><b>Security Notes:</b>
     * <ul>
     *   <li>/login: Credentials in body, no cookies yet → CSRF safe</li>
     *   <li>/initiate-login: First step of login → CSRF safe</li>
     *   <li>/refresh-token: Uses refresh token cookie → Should have CSRF in production</li>
     * </ul>
     * 
     * <p><b>⚠️ Important:</b> refresh-token is public but uses cookies.
     * In production, it should NOT be in CSRF_IGNORE.
     */
    public static final String[] PUBLIC = {
        "/api/auth/login",
        "/api/auth/initiate-login",
        "/api/auth/refresh-token"
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated routes requiring valid user session.
     * 
     * <p>These routes require a valid access token in HttpOnly cookie.
     * All of them MUST have CSRF protection enabled.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/auth/logout - Terminate session and invalidate tokens</li>
     *   <li>GET  /api/auth/me - Get current authenticated user information</li>
     *   <li>GET  /api/auth/session - Get current session details (device, login time, etc.)</li>
     *   <li>GET  /api/auth/status - Check authentication status and session validity</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ All use HttpOnly cookies for authentication</li>
     *   <li>✅ CSRF protection is ACTIVE (not in CSRF_IGNORE)</li>
     *   <li>✅ Logout invalidates tokens and clears cookies</li>
     *   <li>✅ Session endpoints reveal no sensitive data (only current user's info)</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>/logout: User wants to sign out</li>
     *   <li>/me: Frontend needs user profile for UI</li>
     *   <li>/session: Display session info in settings</li>
     *   <li>/status: Check if session is still valid before sensitive operations</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
        "/api/auth/logout",
        "/api/auth/me",
        "/api/auth/session",
        "/api/auth/status"
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ /login: No cookies yet, credentials in body → Safe</li>
     *   <li>✅ /initiate-login: First step, no session → Safe</li>
     *   <li>⚠️ /refresh-token: Uses cookie but currently exempted for compatibility</li>
     *   <li>❌ NEVER /logout, /me, /session, /status (use cookies)</li>
     * </ul>
     * 
     * <p><b>Why /login and /initiate-login are safe:</b>
     * <ul>
     *   <li>No prior authentication or cookies exist</li>
     *   <li>Credentials come from request body, not cookies</li>
     *   <li>CSRF attacks require an existing authenticated session</li>
     *   <li>Rate limiting prevents brute force attacks</li>
     * </ul>
     * 
     * <p><b>⚠️ Production TODO:</b>
     * <ul>
     *   <li>Consider adding CSRF to /refresh-token (uses refresh token cookie)</li>
     *   <li>Ensure /logout, /me, /session, /status are NOT in this list</li>
     *   <li>Remove any temporary development exceptions</li>
     * </ul>
     * 
     * <p><b>Current Temporary Exceptions (REMOVE IN PROD):</b>
     * The following routes are temporarily in CSRF_IGNORE for development/testing.
     * They MUST be removed before production as they use HttpOnly cookies:
     * <ul>
     *   <li>/api/auth/logout ← Uses cookies, needs CSRF</li>
     *   <li>/api/auth/me ← Uses cookies, needs CSRF</li>
     *   <li>/api/auth/session ← Uses cookies, needs CSRF</li>
     *   <li>/api/auth/status ← Uses cookies, needs CSRF</li>
     * </ul>
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ Safe - No cookies, credentials in body
        "/api/auth/login",
        "/api/auth/initiate-login",
        
        // ⚠️ Currently exempted but uses cookies - consider adding CSRF in production
        "/api/auth/refresh-token"
        
        // TODO PRODUCTION: Ensure these are NOT here (they're in old SecurityConstants)
        // "/api/auth/logout",          // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/me",              // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/session",         // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
        // "/api/auth/status"           // ⚠️ DEVELOPMENT ONLY - REMOVE IN PROD
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private AuthRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}