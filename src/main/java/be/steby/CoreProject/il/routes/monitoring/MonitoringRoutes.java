package be.steby.CoreProject.il.routes.monitoring;

/**
 * Security routes configuration for Monitoring domain.
 * 
 * These routes require specific MONITORING authority (or SUPER_ADMIN).
 * They are NOT in AUTHENTICATED_ROUTES because not all authenticated users can access them.
 * 
 * Used in SecurityConfig with:
 * .requestMatchers(MONITORING_AUTHORIZED_ROUTES).hasAnyAuthority("MONITORING", "SUPER_ADMIN")
 */
public final class MonitoringRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    public static final String[] PUBLIC = {
        // No public monitoring routes
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * No regular authenticated routes - monitoring requires specific MONITORING role.
     */
    public static final String[] AUTHENTICATED = {
        // No regular authenticated routes
    };
    
    // ========== MONITORING AUTHORIZED ROUTES ==========
    
    /**
     * Routes requiring MONITORING or SUPER_ADMIN authority.
     * 
     * These routes expose application monitoring data and metrics.
     * They're separate from ADMIN_ROUTES because they can be accessed
     * by users with just the MONITORING role (not full admin).
     */
    public static final String[] MONITORING_AUTHORIZED = {
        "/api/monitoring/**"
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * ⚠️ TODO PRODUCTION: This should be EMPTY!
     * Monitoring routes use cookies and need CSRF protection.
     */
    public static final String[] CSRF_IGNORE = {
        // ⚠️ DEV ONLY - Remove in production (uses cookies)
        "/api/monitoring/**"
    };
    
    private MonitoringRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}