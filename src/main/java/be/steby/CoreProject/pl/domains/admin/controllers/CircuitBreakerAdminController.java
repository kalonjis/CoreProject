package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.dal.repositories.FailedEmailRepository;
import be.steby.CoreProject.dl.enums.FailedEmailStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin endpoints for Circuit Breaker management.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/circuit-breaker")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")  // ← FIXED
public class CircuitBreakerAdminController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final FailedEmailRepository failedEmailRepository;

    /**
     * Get status of all Circuit Breakers.
     * GET /api/admin/circuit-breaker/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAllStatus() {
        Map<String, Object> response = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().name());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate());
            cbStatus.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            cbStatus.put("numberOfBufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            cbStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            cbStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("numberOfSlowCalls", cb.getMetrics().getNumberOfSlowCalls());
            cbStatus.put("numberOfNotPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls());

            response.put(cb.getName(), cbStatus);
        });

        return ResponseEntity.ok(response);
    }

    /**
     * Get status of a specific Circuit Breaker.
     * GET /api/admin/circuit-breaker/{name}/status
     */
    @GetMapping("/{name}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);

        Map<String, Object> status = new HashMap<>();
        status.put("name", cb.getName());
        status.put("state", cb.getState().name());
        status.put("failureRate", cb.getMetrics().getFailureRate() + "%");
        status.put("slowCallRate", cb.getMetrics().getSlowCallRate() + "%");
        status.put("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
        status.put("failedCalls", cb.getMetrics().getNumberOfFailedCalls());
        status.put("successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
        status.put("notPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls());

        return ResponseEntity.ok(status);
    }

    /**
     * Manually reset a Circuit Breaker to CLOSED state.
     * POST /api/admin/circuit-breaker/{name}/reset
     */
    @PostMapping("/{name}/reset")
    public ResponseEntity<Map<String, String>> reset(@PathVariable String name) {
        log.warn("Admin manually resetting Circuit Breaker: {}", name);

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        cb.reset();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Circuit Breaker '" + name + "' has been reset");
        response.put("newState", cb.getState().name());

        return ResponseEntity.ok(response);
    }

    /**
     * Force a Circuit Breaker to OPEN state (for testing).
     * POST /api/admin/circuit-breaker/{name}/open
     */
    @PostMapping("/{name}/open")
    public ResponseEntity<Map<String, String>> forceOpen(@PathVariable String name) {
        log.warn("Admin forcing Circuit Breaker OPEN: {}", name);

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        cb.transitionToOpenState();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Circuit Breaker '" + name + "' forced to OPEN");
        response.put("newState", cb.getState().name());

        return ResponseEntity.ok(response);
    }

    /**
     * Force a Circuit Breaker to CLOSED state.
     * POST /api/admin/circuit-breaker/{name}/close
     */
    @PostMapping("/{name}/close")
    public ResponseEntity<Map<String, String>> forceClose(@PathVariable String name) {
        log.warn("Admin forcing Circuit Breaker CLOSED: {}", name);

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        cb.transitionToClosedState();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Circuit Breaker '" + name + "' forced to CLOSED");
        response.put("newState", cb.getState().name());

        return ResponseEntity.ok(response);
    }

    /**
     * Get failed email queue statistics.
     * GET /api/admin/circuit-breaker/email-stats
     */
    @GetMapping("/email-stats")
    public ResponseEntity<Map<String, Object>> getEmailStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("pending", failedEmailRepository.countByStatus(FailedEmailStatus.PENDING));
        stats.put("retrying", failedEmailRepository.countByStatus(FailedEmailStatus.RETRYING));
        stats.put("sent", failedEmailRepository.countByStatus(FailedEmailStatus.SENT));
        stats.put("failed", failedEmailRepository.countByStatus(FailedEmailStatus.FAILED));

        CircuitBreaker smtpCb = circuitBreakerRegistry.circuitBreaker("smtpBackend");
        stats.put("smtpCircuitBreakerState", smtpCb.getState().name());

        return ResponseEntity.ok(stats);
    }
}