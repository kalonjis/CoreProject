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

            // Actions - Single
            "/api/notifications/*/read",
            "/api/notifications/*/dismiss",

            // Actions - Bulk
            "/api/notifications/read",           // POST: Mark multiple as read
            "/api/notifications/read-all",
            "/api/notifications/delete",         // POST: Delete multiple
            "/api/notifications/dismiss-all",

            // Preferences - Read
            "/api/notifications/preferences",
            "/api/notifications/preferences/matrix",
            "/api/notifications/preferences/channel/*",     // GET: By channel
            "/api/notifications/preferences/quiet-hours/*", // GET & DELETE

            // Preferences - Update
            "/api/notifications/preferences/bulk",
            "/api/notifications/preferences/quiet-hours",
            "/api/notifications/preferences/mute",
            "/api/notifications/preferences/type/*/enable-all",   // POST
            "/api/notifications/preferences/type/*/disable-all",  // POST
            "/api/notifications/preferences/reset"
    };

    // ========== CSRF CONFIGURATION ==========

    /**
     * Routes that bypass CSRF protection.
     *
     * ⚠️ TODO PRODUCTION: Remove all routes - notifications use cookies.
     */
    public static final String[] CSRF_IGNORE = {
            // SSE Stream
            "/api/notifications/stream",

            // List & Query
            "/api/notifications",
            "/api/notifications/active",
            "/api/notifications/unread",
            "/api/notifications/unread/count",
            "/api/notifications/*",

            // Actions - Single
            "/api/notifications/*/read",
            "/api/notifications/*/dismiss",

            // Actions - Bulk
            "/api/notifications/read",           // POST: Mark multiple as read
            "/api/notifications/read-all",
            "/api/notifications/delete",         // POST: Delete multiple
            "/api/notifications/dismiss-all",

            // Preferences - Read
            "/api/notifications/preferences",
            "/api/notifications/preferences/matrix",
            "/api/notifications/preferences/channel/*",     // GET: By channel
            "/api/notifications/preferences/quiet-hours/*", // GET & DELETE

            // Preferences - Update
            "/api/notifications/preferences/bulk",
            "/api/notifications/preferences/quiet-hours",
            "/api/notifications/preferences/mute",
            "/api/notifications/preferences/type/*/enable-all",   // POST
            "/api/notifications/preferences/type/*/disable-all",  // POST
            "/api/notifications/preferences/reset"
    };

    private NotificationRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}