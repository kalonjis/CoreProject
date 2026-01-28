package be.steby.CoreProject.il.routes.profile;

/**
 * Security routes configuration for Profile domain.
 *
 * <p>Defines access rules for user profile management endpoints.</p>
 *
 * <p>Route categories:</p>
 * <ul>
 *   <li>PUBLIC: None - all profile routes require authentication</li>
 *   <li>AUTHENTICATED: Avatar upload/removal, profile updates</li>
 * </ul>
 *
 * @see be.steby.CoreProject.il.routes.SecurityRoutesAggregator
 * @see be.steby.CoreProject.pl.domains.profile.avatar.controller.AvatarController
 */
public final class ProfileRoutes {

    // ========== PUBLIC ROUTES ==========

    /**
     * Public profile routes.
     *
     * <p>Currently empty - all profile operations require authentication.</p>
     */
    public static final String[] PUBLIC = {
            // No public routes
    };

    // ========== AUTHENTICATED ROUTES ==========

    /**
     * Routes requiring authentication (any logged-in user).
     *
     * <p>Includes:</p>
     * <ul>
     *   <li>POST   /api/profile/avatar → Upload and set avatar</li>
     *   <li>DELETE /api/profile/avatar → Remove avatar</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
            "/api/profile/avatar"   // POST: Upload avatar, DELETE: Remove avatar
    };

    // ========== ADMIN ROUTES ==========

    /**
     * Admin profile routes.
     *
     * <p>Currently empty - admin user management is in AdminRoutes.</p>
     */
    public static final String[] ADMIN = {
            // No admin routes here
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * <p>Avatar upload uses multipart/form-data which can have CSRF issues.
     * The endpoint is protected by authentication.</p>
     */
    public static final String[] CSRF_IGNORE = {
            "/api/profile/avatar"   // Multipart upload
    };

    private ProfileRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}