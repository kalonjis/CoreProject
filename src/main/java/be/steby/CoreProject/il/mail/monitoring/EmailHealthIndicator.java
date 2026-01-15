package be.steby.CoreProject.il.mail.monitoring;

import be.steby.CoreProject.dal.repositories.FailedEmailRepository;
import be.steby.CoreProject.dl.enums.FailedEmailStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the email subsystem.
 * 
 * Reports health based on:
 * - Circuit Breaker state (CLOSED = healthy, OPEN = degraded)
 * - Number of failed emails pending retry
 * - Number of permanently failed emails (requiring attention)
 * 
 * Visible at: GET /actuator/health
 */
@Component("emailSystem")
@RequiredArgsConstructor
public class EmailHealthIndicator implements HealthIndicator {

    private static final String CIRCUIT_BREAKER_NAME = "smtpBackend";
    private static final int FAILED_THRESHOLD = 10;  // Alert if more than 10 permanently failed

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final FailedEmailRepository failedEmailRepository;

    @Override
    public Health health() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        CircuitBreaker.State state = circuitBreaker.getState();

        // Get queue stats
        long pendingCount = failedEmailRepository.countByStatus(FailedEmailStatus.PENDING);
        long retryingCount = failedEmailRepository.countByStatus(FailedEmailStatus.RETRYING);
        long failedCount = failedEmailRepository.countByStatus(FailedEmailStatus.FAILED);

        // Build health details
        Health.Builder builder = Health.up()
                .withDetail("circuitBreaker", state.name())
                .withDetail("failureRate", circuitBreaker.getMetrics().getFailureRate() + "%")
                .withDetail("pendingRetries", pendingCount)
                .withDetail("currentlyRetrying", retryingCount)
                .withDetail("permanentlyFailed", failedCount);

        // Determine health status
        if (state == CircuitBreaker.State.OPEN) {
            builder.status("DEGRADED")
                    .withDetail("message", "SMTP Circuit Breaker is OPEN - emails queued for retry");
        } else if (state == CircuitBreaker.State.HALF_OPEN) {
            builder.status("RECOVERING")
                    .withDetail("message", "SMTP Circuit Breaker is testing connectivity");
        } else if (failedCount >= FAILED_THRESHOLD) {
            builder.status("WARNING")
                    .withDetail("message", failedCount + " emails permanently failed - manual intervention needed");
        }

        return builder.build();
    }
}