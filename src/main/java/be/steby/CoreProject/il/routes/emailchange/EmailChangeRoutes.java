package be.steby.CoreProject.il.routes.emailchange;

/**
 * Security routes configuration for Email Change domain.
 * 
 * <p>This class defines security routes for the email change process, which is
 * a critical security operation requiring multi-step verification to prevent
 * account takeover attacks.
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>Email change request initiation</li>
 *   <li>Old email verification (prove ownership)</li>
 *   <li>New email confirmation (verify new email)</li>
 *   <li>Email change cancellation</li>
 * </ul>
 * 
 * <p><b>Email Change Flow (3-Step Verification):</b>
 * <ol>
 *   <li><b>Request:</b> User initiates change → POST /request (authenticated)</li>
 *   <li><b>Verify Old:</b> Link sent to OLD email → GET /verification (proves ownership)</li>
 *   <li><b>Confirm New:</b> Link sent to NEW email → GET /confirmation (confirms new email)</li>
 *   <li><b>Complete:</b> Email updated, sessions invalidated</li>
 * </ol>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Request requires authentication (prevent anonymous changes)</li>
 *   <li>Verification uses one-time token sent to OLD email (prove ownership)</li>
 *   <li>Confirmation uses one-time token sent to NEW email (verify new email)</li>
 *   <li>Cancellation available at any step</li>
 *   <li>All active sessions invalidated after email change</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class EmailChangeRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * Public email change routes for token-based verification.
     * 
     * <p>These routes are accessed via email links during the email change process.
     * They use one-time tokens, not cookies, so CSRF can be safely ignored.
     * 
     * <p><b>Verification & Confirmation:</b>
     * <ul>
     *   <li>GET /api/email-address-change/verification - Verify old email (token from old email)</li>
     *   <li>GET /api/email-address-change/confirmation - Confirm new email (token from new email)</li>
     *   <li>GET /api/email-address-change/cancel - Cancel email change process (token from email)</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>One-time tokens sent to user's email addresses</li>
     *   <li>Tokens expire after 24-48 hours</li>
     *   <li>No cookies involved - safe from CSRF</li>
     *   <li>Verification proves ownership of old email</li>
     *   <li>Confirmation proves access to new email</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>User clicks "Verify" link in old email → /verification</li>
     *   <li>User clicks "Confirm" link in new email → /confirmation</li>
     *   <li>User clicks "Cancel" link → /cancel</li>
     * </ul>
     */
    public static final String[] PUBLIC = {
        "/api/email-address-change/cancel",
        "/api/email-address-change/verification",
        "/api/email-address-change/confirmation"
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated email change routes.
     * 
     * <p>These routes allow authenticated users to initiate the email change process.
     * Uses HttpOnly cookies for authentication.
     * 
     * <p><b>Endpoints:</b>
     * <ul>
     *   <li>POST /api/email-address-change/request - Initiate email change (requires password)</li>
     * </ul>
     * 
     * <p><b>Request Requirements:</b>
     * <ul>
     *   <li>Valid authentication (access token cookie)</li>
     *   <li>Current password verification (security confirmation)</li>
     *   <li>New email address (must be valid and not already registered)</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ Uses HttpOnly cookies - CSRF protection REQUIRED in production</li>
     *   <li>✅ Requires password confirmation (prevents session hijacking)</li>
     *   <li>✅ Rate limited (prevents abuse)</li>
     *   <li>✅ Validates new email is not already in use</li>
     *   <li>✅ Sends verification to BOTH old and new emails</li>
     * </ul>
     * 
     * <p><b>Flow After Request:</b>
     * <ol>
     *   <li>System validates request (auth, password, new email)</li>
     *   <li>Creates pending email change record</li>
     *   <li>Sends verification email to OLD email</li>
     *   <li>Waits for user to click verification link</li>
     *   <li>Sends confirmation email to NEW email</li>
     *   <li>Waits for user to click confirmation link</li>
     *   <li>Updates email, invalidates all sessions</li>
     * </ol>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>User changes email in account settings</li>
     *   <li>User migrates to new email provider</li>
     *   <li>User recovers from compromised email</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
        "/api/email-address-change/request"
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ Verification/Confirmation use email tokens - Safe to ignore CSRF</li>
     *   <li>✅ Cancel uses email token - Safe to ignore CSRF</li>
     *   <li>⚠️ Request uses cookies - Should have CSRF in production</li>
     * </ul>
     * 
     * <p><b>Safe Routes (Email Tokens):</b>
     * <ul>
     *   <li>/verification: One-time token from old email → Safe</li>
     *   <li>/confirmation: One-time token from new email → Safe</li>
     *   <li>/cancel: One-time token from email → Safe</li>
     * </ul>
     * 
     * <p><b>⚠️ Edge Case - /request:</b>
     * This endpoint uses HttpOnly cookies for authentication but is currently
     * in CSRF_IGNORE for development. However, it requires password confirmation
     * which provides some CSRF mitigation.
     * 
     * <p><b>Why /request is Currently Exempted:</b>
     * <ul>
     *   <li>Requires password confirmation (not just cookie)</li>
     *   <li>Multi-step verification process (old email + new email)</li>
     *   <li>User must have access to both emails to complete</li>
     *   <li>All sessions invalidated after email change</li>
     * </ul>
     * 
     * <p><b>Production Considerations:</b>
     * <ul>
     *   <li><b>Keep in CSRF_IGNORE:</b> Password confirmation mitigates CSRF risk</li>
     *   <li><b>Alternative:</b> Add CSRF protection for defense-in-depth</li>
     *   <li><b>Trade-off:</b> Password confirmation vs CSRF token</li>
     * </ul>
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify verification/confirmation/cancel use email tokens</li>
     *   <li>⚠️ Decide if /request should have CSRF (currently password-protected)</li>
     *   <li>✅ Ensure password requirement is enforced on /request</li>
     *   <li>✅ Test complete email change flow</li>
     * </ol>
     * 
     * <p><b>Current Configuration:</b>
     * All public routes (email token-based) safely ignore CSRF.
     * /request is in CSRF_IGNORE in old SecurityConstants but could be
     * protected by CSRF for additional security layer.
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ Safe - Email tokens, no cookies
        "/api/email-address-change/cancel",
        "/api/email-address-change/verification",
        "/api/email-address-change/confirmation"
        
        // ⚠️ Currently in old SecurityConstants CSRF_IGNORE but uses cookies:
        // "/api/email-address-change/request"  // Uses cookies + password confirmation
        
        // TODO PRODUCTION: Decide if /request should have CSRF protection
        // Current: Password confirmation provides CSRF mitigation
        // Option: Add CSRF for defense-in-depth
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private EmailChangeRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}