package be.steby.CoreProject.il.sms.monitoring;

import be.steby.CoreProject.dal.repositories.FailedSmsRepository;
import be.steby.CoreProject.dl.enums.FailedSmsStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the SMS subsystem.
 *
 * Reports health based on:
 * - Circuit Breaker state (CLOSED = healthy, OPEN = degraded)
 * - Number of failed SMS pending retry
 * - Number of permanently failed SMS (requiring attention)
 *
 * Visible at: GET /actuator/health
 *
 * @see be.steby.CoreProject.il.sms.TwilioSmsSender
 */
@Component("smsSystem")
@RequiredArgsConstructor
public class SmsHealthIndicator implements HealthIndicator {

    private static final String CIRCUIT_BREAKER_NAME = "twilioBackend";
    private static final int FAILED_THRESHOLD = 10;  // Alert if more than 10 permanently failed

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final FailedSmsRepository failedSmsRepository;

    @Override
    public Health health() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        CircuitBreaker.State state = circuitBreaker.getState();

        // Get queue stats
        long pendingCount = failedSmsRepository.countByStatus(FailedSmsStatus.PENDING);
        long retryingCount = failedSmsRepository.countByStatus(FailedSmsStatus.RETRYING);
        long failedCount = failedSmsRepository.countByStatus(FailedSmsStatus.FAILED);

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
                    .withDetail("message", "Twilio Circuit Breaker is OPEN - SMS queued for retry");
        } else if (state == CircuitBreaker.State.HALF_OPEN) {
            builder.status("RECOVERING")
                    .withDetail("message", "Twilio Circuit Breaker is testing connectivity");
        } else if (failedCount >= FAILED_THRESHOLD) {
            builder.status("WARNING")
                    .withDetail("message", failedCount + " SMS permanently failed - manual intervention needed");
        }

        return builder.build();
    }
}