package be.steby.CoreProject.pl.domains.monitoring.controllers;

import be.steby.CoreProject.bll.domains.monitoring.services.MonitoringService;
import be.steby.CoreProject.pl.domains.monitoring.models.mappers.MonitoringMapper;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.CircuitBreakerStatusResponse;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.DashboardMetricsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Monitoring API Controller.
 *
 * Provides system health and metrics endpoints for Ops/IT staff.
 * Separated from admin domain for proper SoC.
 *
 * Accessible by: MONITORING, ADMIN, SUPER_ADMIN roles.
 *
 * Endpoints:
 * - GET /api/monitoring/health                    → Full health status
 * - GET /api/monitoring/health/{component}        → Component health
 * - GET /api/monitoring/metrics/dashboard         → Aggregated metrics
 * - GET /api/monitoring/circuit-breakers          → All circuit breakers
 * - GET /api/monitoring/circuit-breakers/{name}   → Specific circuit breaker
 */
@RestController
@RequestMapping("/api/monitoring")
@PreAuthorize("hasAuthority('MONITORING')")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final MonitoringMapper monitoringMapper;

    // =========================================================================
    // HEALTH ENDPOINTS
    // =========================================================================

    /**
     * Get full application health status with all components.
     *
     * GET /api/monitoring/health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthComponent> getHealth() {
        log.debug("Requesting full health status");
        return ResponseEntity.ok(monitoringService.getHealth());
    }

    /**
     * Get health status of a specific component.
     *
     * GET /api/monitoring/health/{component}
     *
     * @param component Component name (db, diskSpace, emailSystem, etc.)
     */
    @GetMapping("/health/{component}")
    public ResponseEntity<HealthComponent> getComponentHealth(@PathVariable String component) {
        log.debug("Requesting health for component: {}", component);
        return ResponseEntity.ok(monitoringService.getComponentHealth(component));
    }

    // =========================================================================
    // METRICS ENDPOINTS
    // =========================================================================

    /**
     * Get aggregated metrics for the monitoring dashboard.
     *
     * GET /api/monitoring/metrics/dashboard
     */
    @GetMapping("/metrics/dashboard")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        log.debug("Requesting dashboard metrics");

        var metrics = monitoringService.getDashboardMetrics();
        var response = monitoringMapper.toResponse(metrics);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // CIRCUIT BREAKER ENDPOINTS
    // =========================================================================

    /**
     * Get status of all circuit breakers.
     *
     * GET /api/monitoring/circuit-breakers
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<List<CircuitBreakerStatusResponse>> getAllCircuitBreakers() {
        log.debug("Requesting all circuit breakers status");

        var circuitBreakers = monitoringService.getAllCircuitBreakers();
        var response = monitoringMapper.toResponseList(circuitBreakers);

        return ResponseEntity.ok(response);
    }

    /**
     * Get status of a specific circuit breaker.
     *
     * GET /api/monitoring/circuit-breakers/{name}
     *
     * @param name Circuit breaker name (e.g., 'smtpBackend', 'twilioBackend')
     */
    @GetMapping("/circuit-breakers/{name}")
    public ResponseEntity<CircuitBreakerStatusResponse> getCircuitBreaker(@PathVariable String name) {
        log.debug("Requesting circuit breaker: {}", name);

        var circuitBreaker = monitoringService.getCircuitBreaker(name);
        var response = monitoringMapper.toResponse(circuitBreaker);

        return ResponseEntity.ok(response);
    }
}