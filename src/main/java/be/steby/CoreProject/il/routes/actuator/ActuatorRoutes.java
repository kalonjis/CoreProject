package be.steby.CoreProject.il.routes.actuator;

/**
 * Security routes configuration for Spring Boot Actuator endpoints.
 * 
 * <p>This class defines security routes for application monitoring, health checks,
 * and management operations. Actuator endpoints expose application internals
 * and must be carefully secured based on sensitivity level.
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>Health checks for load balancers and monitoring</li>
 *   <li>Application metrics and statistics</li>
 *   <li>Configuration and environment information</li>
 *   <li>Circuit breaker and resilience status</li>
 *   <li>Log level management</li>
 * </ul>
 * 
 * <p><b>Security Levels:</b>
 * <ul>
 *   <li><b>Public:</b> Health checks, basic info (safe for load balancers)</li>
 *   <li><b>Authenticated:</b> Metrics, prometheus (safe operational data)</li>
 *   <li><b>Admin:</b> Configuration, environment, log management (sensitive)</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Public endpoints expose minimal information</li>
 *   <li>Authenticated endpoints require valid session</li>
 *   <li>Admin endpoints require ADMIN role and CSRF protection</li>
 *   <li>Sensitive data (passwords, secrets) filtered from responses</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class ActuatorRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * Public actuator routes for health checks and basic info.
     * 
     * <p>These endpoints are safe to expose publicly as they contain minimal
     * information and are used by load balancers, monitoring tools, and health checks.
     * 
     * <p><b>Health Endpoints:</b>
     * <ul>
     *   <li>GET /actuator/health - Overall application health status</li>
     *   <li>GET /actuator/health/** - Detailed health checks (liveness, readiness)</li>
     *   <li>GET /actuator/info - Basic application information (version, name)</li>
     * </ul>
     * 
     * <p><b>Monitoring:</b>
     * <ul>
     *   <li>GET /actuator/prometheus - Prometheus metrics endpoint</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>No authentication required (safe for monitoring tools)</li>
     *   <li>Minimal information exposure</li>
     *   <li>No sensitive data revealed</li>
     *   <li>Safe for load balancer health checks</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Load balancer checks /health before routing traffic</li>
     *   <li>Kubernetes liveness/readiness probes</li>
     *   <li>Prometheus scrapes /prometheus for metrics</li>
     *   <li>Public status page shows application version</li>
     * </ul>
     */
    public static final String[] PUBLIC = {
        "/actuator/health",           // Overall health
        "/actuator/health/**",        // Detailed health (liveness, readiness)
        "/actuator/info",             // Application info
        "/actuator/prometheus"        // Prometheus scraping — public in Spring, restricted at nginx/firewall level (VPS internal only)
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated actuator routes for operational metrics.
     * 
     * <p>These endpoints provide operational data useful for monitoring and
     * troubleshooting but don't expose sensitive configuration or allow
     * state changes. They require authentication but not admin privileges.
     * 
     * <p><b>Metrics:</b>
     * <ul>
     *   <li>GET /actuator/metrics - List available metrics</li>
     *   <li>GET /actuator/metrics/** - Specific metric details</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ Requires authentication (access token cookie)</li>
     *   <li>✅ CSRF protection ACTIVE (uses cookies)</li>
     *   <li>✅ Read-only operations (no state changes)</li>
     *   <li>✅ No sensitive data exposure</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Developers check application metrics</li>
     *   <li>Operations team monitors performance</li>
     *   <li>Troubleshooting performance issues</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
        "/actuator/metrics",
        "/actuator/metrics/**"
    };
    
    // ========== ADMIN ROUTES ==========
    
    /**
     * Admin-only actuator routes for sensitive operations.
     * 
     * <p>These endpoints expose sensitive application internals or allow
     * state changes. They require ADMIN or SUPER_ADMIN role and MUST have
     * CSRF protection enabled.
     * 
     * <p><b>Actuator Index:</b>
     * <ul>
     *   <li>GET /actuator - Actuator index (lists all endpoints)</li>
     * </ul>
     * 
     * <p><b>Resilience (Circuit Breakers, Retries):</b>
     * <ul>
     *   <li>GET /actuator/circuitbreakers - Circuit breaker status</li>
     *   <li>GET /actuator/circuitbreakers/** - Specific circuit breaker details</li>
     *   <li>GET /actuator/circuitbreakerevents - Circuit breaker events</li>
     *   <li>GET /actuator/circuitbreakerevents/** - Specific circuit breaker events</li>
     *   <li>GET /actuator/retries - Retry status</li>
     *   <li>GET /actuator/retries/** - Specific retry details</li>
     *   <li>GET /actuator/retryevents - Retry events</li>
     *   <li>GET /actuator/retryevents/** - Specific retry events</li>
     * </ul>
     * 
     * <p><b>Configuration & Environment:</b>
     * <ul>
     *   <li>GET /actuator/env - Environment properties (sensitive!)</li>
     *   <li>GET /actuator/env/** - Specific environment property</li>
     *   <li>GET /actuator/configprops - Configuration properties</li>
     *   <li>GET /actuator/beans - Spring beans information</li>
     *   <li>GET /actuator/mappings - Request mappings</li>
     * </ul>
     * 
     * <p><b>Logging:</b>
     * <ul>
     *   <li>GET  /actuator/loggers - Logger configuration</li>
     *   <li>POST /actuator/loggers/** - Change log levels (state change!)</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ Requires ADMIN or SUPER_ADMIN authority</li>
     *   <li>✅ CSRF protection ALWAYS ACTIVE (never bypassed)</li>
     *   <li>✅ Sensitive data filtered (passwords, API keys)</li>
     *   <li>✅ State-changing operations (log levels) protected</li>
     *   <li>❌ NEVER in CSRF_IGNORE</li>
     * </ul>
     * 
     * <p><b>Why Admin Routes Need CSRF:</b>
     * <ul>
     *   <li>Expose sensitive configuration data</li>
     *   <li>Allow state changes (log levels)</li>
     *   <li>Could leak environment variables, API keys</li>
     *   <li>CSRF attack could change logging levels → hide attacks</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Admin checks circuit breaker status</li>
     *   <li>DevOps reviews environment configuration</li>
     *   <li>Admin temporarily increases log level for debugging</li>
     *   <li>Operations checks bean configuration</li>
     * </ul>
     */
    public static final String[] ADMIN = {
        // Actuator Index
        "/actuator",
        
        // Resilience4j Circuit Breakers
        "/actuator/circuitbreakers",
        "/actuator/circuitbreakers/**",
        "/actuator/circuitbreakerevents",
        "/actuator/circuitbreakerevents/**",
        
        // Resilience4j Retries
        "/actuator/retries",
        "/actuator/retries/**",
        "/actuator/retryevents",
        "/actuator/retryevents/**",
        
        // Configuration & Environment (SENSITIVE!)
        "/actuator/env",
        "/actuator/env/**",
        "/actuator/configprops",
        "/actuator/beans",
        "/actuator/mappings",
        
        // Logging (includes state-changing operations)
        "/actuator/loggers",
        "/actuator/loggers/**"
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>CRITICAL SECURITY RULES:</b>
     * <ul>
     *   <li>✅ ONLY public routes (no authentication)</li>
     *   <li>❌ NEVER authenticated routes (use cookies)</li>
     *   <li>❌ NEVER admin routes (highly sensitive)</li>
     * </ul>
     * 
     * <p><b>Safe Routes (Public):</b>
     * <ul>
     *   <li>/health: No auth, read-only, safe for load balancers → Safe</li>
     *   <li>/info: No auth, read-only, public information → Safe</li>
     *   <li>/prometheus: No auth, read-only, metrics scraping → Safe</li>
     * </ul>
     * 
     * <p><b>⚠️ NEVER Bypass CSRF For:</b>
     * <ul>
     *   <li>ALL authenticated actuator routes (use cookies)</li>
     *   <li>ALL admin actuator routes (sensitive + state-changing)</li>
     *   <li>/loggers/** (can change log levels - state change)</li>
     *   <li>/env (exposes sensitive configuration)</li>
     * </ul>
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify only /health, /info, /prometheus bypass CSRF</li>
     *   <li>✅ Ensure ALL authenticated routes have CSRF protection</li>
     *   <li>✅ Ensure ALL admin routes have CSRF protection</li>
     *   <li>✅ Test admin operations with CSRF tokens</li>
     * </ol>
     * 
     * <p><b>Current Configuration:</b>
     * Only public, read-only, non-authenticated endpoints bypass CSRF.
     * This is correct and secure.
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ Safe - Public, no auth, read-only
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info",
        "/actuator/prometheus"        // Prometheus scraping — restricted at nginx/firewall level (VPS internal only)
        
        // ❌ NEVER ADD:
        // "/actuator/metrics"         - Uses cookies, needs CSRF
        // "/actuator"                 - Admin only, needs CSRF
        // "/actuator/env"             - Admin only, SENSITIVE, needs CSRF
        // "/actuator/loggers"         - Admin only, STATE-CHANGING, needs CSRF
        // "/actuator/circuitbreakers" - Admin only, needs CSRF
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private ActuatorRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}