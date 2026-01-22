package be.steby.CoreProject.il.routes.account;

/**
 * Security routes configuration for Account Management domain.
 * 
 * <p>This class defines security routes for account lifecycle operations including
 * registration (signup), activation, reactivation, and deactivation.
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>User registration and account creation</li>
 *   <li>Email-based account activation</li>
 *   <li>Account reactivation after deactivation</li>
 *   <li>User-initiated account deactivation</li>
 * </ul>
 * 
 * <p><b>Account Lifecycle:</b>
 * <ol>
 *   <li><b>Signup:</b> User creates account → receives activation email</li>
 *   <li><b>Activation:</b> User clicks email link → account activated</li>
 *   <li><b>Active:</b> Normal account usage</li>
 *   <li><b>Deactivation:</b> User requests deactivation → confirms via email → account deactivated</li>
 *   <li><b>Reactivation:</b> Deactivated user requests reactivation → confirms via email → account active again</li>
 * </ol>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Activation/reactivation use one-time tokens from email - CSRF can be ignored</li>
 *   <li>Deactivation requires authentication and confirmation - needs careful CSRF handling</li>
 *   <li>All email tokens expire after a short period (24-48 hours typically)</li>
 *   <li>Deactivation is soft delete - data retained for potential reactivation</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class AccountRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * Public account routes accessible without authentication.
     * 
     * <p>These routes handle account creation and email-based verification flows.
     * They use one-time tokens sent via email, not cookies, so CSRF can be safely ignored.
     * 
     * <p><b>Signup & Activation:</b>
     * <ul>
     *   <li>POST /api/account/signup - Create new account and send activation email</li>
     *   <li>GET  /api/account/activate/** - Activate account via email token</li>
     *   <li>POST /api/account/resend-activation/** - Resend activation email</li>
     * </ul>
     * 
     * <p><b>Reactivation (After Deactivation):</b>
     * <ul>
     *   <li>POST /api/account/request-reactivation - Request to reactivate a deactivated account</li>
     *   <li>GET  /api/account/confirm-reactivation/** - Confirm reactivation via email token</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>All tokens are one-time use with expiration</li>
     *   <li>Tokens are cryptographically secure random strings</li>
     *   <li>Email verification prevents automated bot signups</li>
     *   <li>Rate limiting on signup and resend endpoints</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>New user creates account → /signup</li>
     *   <li>User clicks activation link in email → /activate/**</li>
     *   <li>Activation email lost/expired → /resend-activation/**</li>
     *   <li>Previously deactivated user wants back → /request-reactivation → /confirm-reactivation/**</li>
     * </ul>
     */
    public static final String[] PUBLIC = {
        "/api/account/signup",
        "/api/account/activate/**",
        "/api/account/resend-activation/**",
        "/api/account/request-reactivation",
        "/api/account/confirm-reactivation/**"
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated account routes requiring valid user session.
     * 
     * <p>These routes allow authenticated users to deactivate their own accounts.
     * Deactivation is a sensitive operation requiring confirmation.
     * 
     * <p><b>Account Deactivation Flow:</b>
     * <ol>
     *   <li>Authenticated user requests deactivation → POST /request-deactivation</li>
     *   <li>System sends confirmation email with token</li>
     *   <li>User clicks email link → GET /confirm-deactivation/**</li>
     *   <li>Account is soft-deleted (data retained for recovery)</li>
     * </ol>
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/account/request-deactivation - Initiate account deactivation (requires auth)</li>
     *   <li>GET  /api/account/confirm-deactivation/** - Confirm deactivation via email token</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>/request-deactivation: Requires authentication, uses cookies → NEEDS CSRF in production</li>
     *   <li>/confirm-deactivation: Uses email token, no cookies → Can ignore CSRF</li>
     *   <li>Two-step process prevents accidental deactivation</li>
     *   <li>Deactivation is reversible via reactivation flow</li>
     * </ul>
     * 
     * <p><b>⚠️ Important Note:</b>
     * /confirm-deactivation/** is in AUTHENTICATED_ROUTES but uses an email token,
     * not cookies. It could technically be in PUBLIC_ROUTES, but we keep it here
     * to be explicit about the deactivation flow.
     */
    public static final String[] AUTHENTICATED = {
        "/api/account/request-deactivation",
        "/api/account/confirm-deactivation/**"
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ Public routes using email tokens - Safe to ignore CSRF</li>
     *   <li>⚠️ /request-deactivation uses cookies but currently exempted</li>
     *   <li>✅ /confirm-deactivation uses email token - Safe to ignore CSRF</li>
     * </ul>
     * 
     * <p><b>Why Public Routes Are Safe:</b>
     * <ul>
     *   <li>/signup: No prior session, data in request body</li>
     *   <li>/activate/**: One-time token from email, no cookies</li>
     *   <li>/resend-activation/**: Token validation, no cookies</li>
     *   <li>/request-reactivation: No authentication, email-based</li>
     *   <li>/confirm-reactivation/**: One-time token from email</li>
     *   <li>/confirm-deactivation/**: One-time token from email</li>
     * </ul>
     * 
     * <p><b>⚠️ Edge Case - /request-deactivation:</b>
     * This endpoint is currently in CSRF_IGNORE for development but uses cookies.
     * <ul>
     *   <li><b>Current:</b> Bypasses CSRF for testing convenience</li>
     *   <li><b>Production:</b> Should have CSRF protection (uses HttpOnly cookie)</li>
     *   <li><b>Mitigation:</b> Two-step confirmation prevents CSRF abuse</li>
     * </ul>
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify all public token-based routes are safe</li>
     *   <li>⚠️ Consider enabling CSRF for /request-deactivation</li>
     *   <li>✅ Ensure /confirm-deactivation stays in CSRF_IGNORE (uses email token)</li>
     *   <li>✅ Test deactivation flow with CSRF enabled</li>
     * </ol>
     * 
     * <p><b>Current Configuration:</b>
     * All routes except /request-deactivation clearly use email tokens.
     * /request-deactivation is a special case - it's authenticated but leads
     * to an email confirmation, so CSRF risk is mitigated by the two-step process.
     * 
     * <p><b>TODO PRODUCTION:</b>
     * Remove /request-deactivation and /confirm-deactivation/** from CSRF_IGNORE.
     * These should have CSRF protection in production since they're part of
     * the authenticated account management flow.
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ Safe - Email tokens, no cookies
        "/api/account/signup",
        "/api/account/activate/**",
        "/api/account/resend-activation/**",
        "/api/account/request-reactivation",
        "/api/account/confirm-reactivation/**"
        
        // ⚠️ Special case - Currently exempted but uses cookies for request, not confirmation
        // "/api/account/request-deactivation",       // TODO PRODUCTION: Remove - uses cookies
        // "/api/account/confirm-deactivation/**"     // Could stay - uses email token
        
        // TODO PRODUCTION: Remove these deactivation routes from CSRF_IGNORE
        // They're in old SecurityConstants for development but should be protected in prod
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private AccountRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}