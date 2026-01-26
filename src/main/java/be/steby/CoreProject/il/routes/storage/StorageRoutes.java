package be.steby.CoreProject.il.routes.storage;

/**
 * Security routes configuration for Storage domain.
 *
 * <p>Defines access rules for file upload, download, and management endpoints.</p>
 *
 * <p>Route categories:
 * <ul>
 *   <li>PUBLIC: Public file access (if enabled per category)</li>
 *   <li>AUTHENTICATED: User file management</li>
 *   <li>ADMIN: Admin file oversight (future)</li>
 * </ul>
 *
 * @see be.steby.CoreProject.il.routes.SecurityRoutesAggregator
 * @see be.steby.CoreProject.pl.domains.storage.controllers.FileController
 */
public final class StorageRoutes {

    // ========== PUBLIC ROUTES ==========

    /**
     * Public storage routes.
     *
     * <p>Currently empty - all file access requires authentication.
     * Future: Could expose public files (avatars, product images) without auth.</p>
     *
     * <p>Example future routes:
     * <pre>
     * "/api/files/public/**"  // Public file serving
     * </pre>
     */
    public static final String[] PUBLIC = {
            // No public routes for now
            // Files are served through authenticated endpoints
    };

    // ========== AUTHENTICATED ROUTES ==========

    /**
     * Routes requiring authentication (any logged-in user).
     *
     * <p>Includes:
     * <ul>
     *   <li>File upload</li>
     *   <li>File download/serving</li>
     *   <li>File metadata queries</li>
     *   <li>File deletion</li>
     *   <li>Storage usage info</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
            "/api/files",              // POST: Upload
            "/api/files/my",           // GET: List my files
            "/api/files/storage/**",   // GET: Storage usage
            "/api/files/*",            // GET: Download, DELETE: Delete
            "/api/files/*/info",       // GET: Metadata
            "/api/files/*/download"    // GET: Force download
    };

    // ========== ADMIN ROUTES ==========

    /**
     * Routes requiring ADMIN or SUPER_ADMIN role.
     *
     * <p>Future: Admin file management capabilities.</p>
     */
    public static final String[] ADMIN = {
            // Future: Admin endpoints
            // "/api/admin/files/**"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * <p>File upload uses multipart/form-data which can have CSRF issues.
     * The upload endpoint is protected by authentication anyway.</p>
     *
     * <p>⚠️ TODO PRODUCTION: Review if this is still needed with proper CSRF token handling.</p>
     */
    public static final String[] CSRF_IGNORE = {
            "/api/files",      // Upload endpoint (multipart)
            "/api/files/*"     // Delete endpoint
    };

    private StorageRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}