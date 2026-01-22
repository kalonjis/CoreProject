package be.steby.CoreProject.bll.domains.monitoring.services;

import be.steby.CoreProject.bll.domains.monitoring.models.CircuitBreakerStatus;
import be.steby.CoreProject.bll.domains.monitoring.models.DashboardMetrics;
import org.springframework.boot.actuate.health.HealthComponent;

import java.util.List;

/**
 * Service for system monitoring operations.
 *
 * Provides access to:
 * - Application health status (via Actuator)
 * - System metrics (JVM, CPU, Disk, DB Pool)
 * - Circuit Breaker status
 */
public interface MonitoringService {

    // =========================================================================
    // HEALTH
    // =========================================================================

    /**
     * Retrieves the full application health status.
     *
     * @return HealthComponent with overall status and component details
     */
    HealthComponent getHealth();

    /**
     * Retrieves health status for a specific component.
     *
     * @param component Component name (db, diskSpace, emailSystem, etc.)
     * @return HealthComponent for the specified component
     */
    HealthComponent getComponentHealth(String component);

    // =========================================================================
    // METRICS
    // =========================================================================

    /**
     * Retrieves aggregated metrics for the monitoring dashboard.
     *
     * @return DashboardMetrics record with all metrics
     */
    DashboardMetrics getDashboardMetrics();

    // =========================================================================
    // CIRCUIT BREAKERS
    // =========================================================================

    /**
     * Retrieves status of all registered circuit breakers.
     *
     * @return List of CircuitBreakerStatus records
     */
    List<CircuitBreakerStatus> getAllCircuitBreakers();

    /**
     * Retrieves status of a specific circuit breaker.
     *
     * @param name Circuit breaker name
     * @return CircuitBreakerStatus record
     * @throws IllegalArgumentException if circuit breaker not found
     */
    CircuitBreakerStatus getCircuitBreaker(String name);
}