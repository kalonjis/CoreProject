package be.steby.CoreProject.il.routes.gdpr;

/**
 * Security routes configuration for the GDPR export domain.
 *
 * <p>Route breakdown:
 * <ul>
 *   <li>PUBLIC: confirm and download — accessed via email links (no session cookie)</li>
 *   <li>AUTHENTICATED: request and status — require a valid session</li>
 * </ul>
 *
 * <p>CSRF notes:
 * <ul>
 *   <li>confirm and download are token-based (email links) → CSRF exempt</li>
 *   <li>request and status use session cookies → CSRF protection kept</li>
 * </ul>
 */
public final class GdprRoutes {

    // =========================================================================
    // Public — accessed via email links, no session required
    // =========================================================================

    public static final String[] PUBLIC = {
            "/api/privacy/export/confirm",   // GET — confirms the export request
            "/api/privacy/export/download"   // GET — downloads the archive
    };

    // =========================================================================
    // Authenticated — require a valid session
    // =========================================================================

    public static final String[] AUTHENTICATED = {
            "/api/privacy/export/request",   // POST — initiates an export request
            "/api/privacy/export/status"     // GET  — polls current request status
    };

    // =========================================================================
    // CSRF — exempt only token-based (email link) routes
    // =========================================================================

    public static final String[] CSRF_IGNORE = {
            "/api/privacy/export/request",   // POST — initiates an export request
            "/api/privacy/export/status" ,
            "/api/privacy/export/confirm",
            "/api/privacy/export/download"
    };

    private GdprRoutes() {
        throw new UnsupportedOperationException("Configuration class — cannot be instantiated");
    }
}