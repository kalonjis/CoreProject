package be.steby.CoreProject.il.routes.swagger;

/**
 * Security routes configuration for Swagger/OpenAPI documentation.
 * 
 * ⚠️ TODO PRODUCTION: Consider adding authentication to Swagger
 * or disabling it entirely in production.
 */
public final class SwaggerRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    public static final String[] PUBLIC = {
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    public static final String[] AUTHENTICATED = {
        // No authenticated routes
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Swagger routes can safely ignore CSRF (read-only documentation).
     */
    public static final String[] CSRF_IGNORE = {
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };
    
    private SwaggerRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}