// src/main/java/be/steby/CoreProject/il/routes/notification/NotificationRoutes.java

package be.steby.CoreProject.il.routes.notification;

/**
 * Security routes configuration for Notification domain.
 */
public final class NotificationRoutes {

    // ========== PUBLIC ROUTES ==========

    /**
     * No public routes - all notification endpoints require authentication.
     */
    public static final String[] PUBLIC = {};

    // ========== AUTHENTICATED ROUTES ==========

    public static final String[] AUTHENTICATED = {
            // SSE Stream
            "/api/notifications/stream",
            
            // List & Query
            "/api/notifications",
            "/api/notifications/active",
            "/api/notifications/unread",
            "/api/notifications/unread/count",
            "/api/notifications/*",
            
            // Actions
            "/api/notifications/*/read",
            "/api/notifications/*/dismiss",
            "/api/notifications/read-all",
            "/api/notifications/dismiss-all",
            
            // Preferences
            "/api/notifications/preferences",
            "/api/notifications/preferences/*",
            "/api/notifications/preferences/*/quiet-hours",
            "/api/notifications/preferences/bulk"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove all routes - notifications use cookies.
     */
    public static final String[] CSRF_IGNORE = {
            // SSE needs CSRF bypass (GET request, long-lived connection)
            "/api/notifications/stream",
            
            // ⚠️ DEV ONLY - Remove in production
            "/api/notifications",
            "/api/notifications/active",
            "/api/notifications/unread",
            "/api/notifications/unread/count",
            "/api/notifications/*",
            "/api/notifications/*/read",
            "/api/notifications/*/dismiss",
            "/api/notifications/read-all",
            "/api/notifications/dismiss-all",
            "/api/notifications/preferences",
            "/api/notifications/preferences/*",
            "/api/notifications/preferences/*/quiet-hours",
            "/api/notifications/preferences/bulk"
    };

    private NotificationRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}