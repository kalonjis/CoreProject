package be.steby.CoreProject.pl.domains.monitoring.controllers;

import be.steby.CoreProject.bll.domains.monitoring.models.SmtpHealthResult;
import be.steby.CoreProject.bll.domains.monitoring.models.TwilioHealthResult;
import be.steby.CoreProject.bll.domains.monitoring.services.MonitoringService;
import be.steby.CoreProject.bll.domains.monitoring.services.SmtpHealthService;
import be.steby.CoreProject.bll.domains.monitoring.services.TwilioHealthService;
import be.steby.CoreProject.pl.domains.monitoring.models.mappers.MonitoringMapper;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.CircuitBreakerStatusResponse;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.DashboardMetricsResponse;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.SmtpHealthResponse;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.TwilioHealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    private final SmtpHealthService smtpHealthService;
    private final MonitoringMapper monitoringMapper;
    private final TwilioHealthService twilioHealthService;

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


    /**
     * Tests SMTP server connectivity.
     *
     * <p>Performs a connection test to the configured SMTP server by executing
     * EHLO/AUTH handshake without sending an actual email.</p>
     *
     * <h4>Response:</h4>
     * <ul>
     *   <li>reachable: true if SMTP server is accessible</li>
     *   <li>responseTimeMs: connection test duration in milliseconds</li>
     *   <li>errorMessage: error details if test failed, null otherwise</li>
     * </ul>
     *
     * @return SmtpHealthResponse with connection test results
     */
    @GetMapping("/smtp/test")
    @Operation(
            summary = "Test SMTP connectivity",
            description = "Tests connection to SMTP server without sending an email"
    )
    @ApiResponse(responseCode = "200", description = "Test completed (check reachable field for result)")
    public ResponseEntity<SmtpHealthResponse> testSmtpConnection() {
        SmtpHealthResult result = smtpHealthService.testConnection();

        return ResponseEntity.ok(new SmtpHealthResponse(
                result.reachable(),
                result.responseTimeMs(),
                result.errorMessage()
        ));
    }


    /**
     * Tests Twilio API connectivity.
     *
     * <p>Fetches account information from Twilio to validate connectivity
     * and credentials without sending an actual SMS.</p>
     *
     * <h4>Response:</h4>
     * <ul>
     *   <li>reachable: true if Twilio API is accessible</li>
     *   <li>responseTimeMs: connection test duration in milliseconds</li>
     *   <li>accountStatus: Twilio account status (active, suspended, closed)</li>
     *   <li>errorMessage: error details if test failed, null otherwise</li>
     * </ul>
     *
     * @return TwilioHealthResponse with connection test results
     */
    @GetMapping("/twilio/test")
    @Operation(
            summary = "Test Twilio connectivity",
            description = "Tests connection to Twilio API without sending an SMS"
    )
    @ApiResponse(responseCode = "200", description = "Test completed (check reachable field for result)")
    public ResponseEntity<TwilioHealthResponse> testTwilioConnection() {
        TwilioHealthResult result = twilioHealthService.testConnection();

        return ResponseEntity.ok(new TwilioHealthResponse(
                result.reachable(),
                result.responseTimeMs(),
                result.accountStatus(),
                result.errorMessage()
        ));
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