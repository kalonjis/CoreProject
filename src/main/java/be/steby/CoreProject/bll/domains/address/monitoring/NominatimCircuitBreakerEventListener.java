package be.steby.CoreProject.bll.domains.address.monitoring;

import be.steby.CoreProject.bll.domains.address.services.geocoding.GeocodingRetryService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listens to Nominatim Circuit Breaker events and logs state transitions.
 *
 * This component provides visibility into Nominatim geocoding service health:
 * - State transitions (CLOSED → OPEN → HALF_OPEN)
 * - Success/failure events
 * - Ignored errors
 * - Rate limiting issues
 *
 * Key feature: Automatically triggers retry of failed geocoding attempts
 * when the circuit breaker recovers (HALF_OPEN → CLOSED).
 *
 * Why Nominatim needs monitoring:
 * - External dependency with strict rate limits (1 req/sec)
 * - Free tier may have availability issues
 * - Timeouts can cascade if circuit breaker doesn't protect
 *
 * Location: src/main/java/be/steby/CoreProject/bll/domains/address/monitoring/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NominatimCircuitBreakerEventListener {

    private static final String NOMINATIM_BACKEND = "nominatimBackend";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final GeocodingRetryService geocodingRetryService;

    /**
     * Registers event listeners for Nominatim circuit breaker.
     * Called automatically after bean construction.
     */
    @PostConstruct
    public void registerEventListeners() {
        CircuitBreaker nominatimCircuitBreaker = circuitBreakerRegistry.circuitBreaker(NOMINATIM_BACKEND);

        nominatimCircuitBreaker.getEventPublisher()
                .onStateTransition(this::onStateTransition)
                .onError(this::onError)
                .onSuccess(this::onSuccess)
                .onIgnoredError(this::onIgnoredError)
                .onReset(this::onReset)
                .onCallNotPermitted(this::onCallNotPermitted);

        log.info("✅ Nominatim Circuit Breaker event listeners registered for '{}'", NOMINATIM_BACKEND);
    }

    /**
     * Called when Circuit Breaker changes state.
     * This is the most critical event for monitoring service health.
     */
    private void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.StateTransition transition = event.getStateTransition();

        switch (transition) {
            case CLOSED_TO_OPEN -> {
                log.error("🔴 CIRCUIT BREAKER OPENED - Nominatim geocoding service is DOWN! " +
                                "Backend: '{}', From: {}, To: {}",
                        event.getCircuitBreakerName(),
                        transition.getFromState(),
                        transition.getToState());
                log.error("⚠️ IMPACT: New addresses will NOT be geocoded until circuit recovers. " +
                        "Manual intervention may be required.");
                // TODO: Send critical alert
                // - Slack/Teams notification
                // - PagerDuty incident
                // - Email to ops team
                // - Switch to backup provider (Google Maps API)
            }

            case OPEN_TO_HALF_OPEN -> {
                CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(event.getCircuitBreakerName());
                log.warn("🟡 CIRCUIT BREAKER HALF-OPEN - Testing Nominatim connectivity. " +
                                "Backend: '{}', Attempting {} test calls",
                        event.getCircuitBreakerName(),
                        cb.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState());
            }

            case HALF_OPEN_TO_CLOSED -> {
                log.info("🟢 CIRCUIT BREAKER CLOSED - Nominatim geocoding service RECOVERED! " +
                                "Backend: '{}', Service is now available",
                        event.getCircuitBreakerName());

                // Trigger retry of all pending failed geocoding attempts
                log.info("🔄 Triggering retry of pending failed geocoding attempts...");
                geocodingRetryService.retryAllPendingGeocoding();
            }

            case HALF_OPEN_TO_OPEN -> {
                log.error("🔴 CIRCUIT BREAKER RE-OPENED - Nominatim still failing after recovery attempt! " +
                                "Backend: '{}', Service remains unavailable",
                        event.getCircuitBreakerName());
                log.error("❌ Recovery test failed. Circuit breaker will wait before retrying again");
                // TODO: Send escalation alert (service still down after recovery attempt)
            }

            case CLOSED_TO_FORCED_OPEN -> {
                log.warn("⚙️ CIRCUIT BREAKER FORCED OPEN - Manual override for '{}'",
                        event.getCircuitBreakerName());
            }

            case FORCED_OPEN_TO_CLOSED, FORCED_OPEN_TO_HALF_OPEN -> {
                log.info("⚙️ CIRCUIT BREAKER MANUAL TRANSITION - From: {}, To: {}",
                        transition.getFromState(), transition.getToState());

                // If transitioning to CLOSED, also trigger retry
                if (transition == CircuitBreaker.StateTransition.FORCED_OPEN_TO_CLOSED) {
                    log.info("🔄 Triggering retry of pending failed geocoding attempts after manual close...");
                    geocodingRetryService.retryAllPendingGeocoding();
                }
            }

            default -> {
                log.debug("Circuit Breaker state transition: {} → {}",
                        transition.getFromState(), transition.getToState());

                // Handle any transition TO CLOSED state (including manual OPEN → CLOSED)
                if (transition.getToState() == CircuitBreaker.State.CLOSED) {
                    log.info("🟢 Circuit breaker transitioned to CLOSED state");
                    log.info("🔄 Triggering retry of pending failed geocoding attempts...");
                    geocodingRetryService.retryAllPendingGeocoding();
                }
            }
        }
    }

    /**
     * Called on each failed geocoding call (recorded as failure).
     * Helps identify patterns in geocoding failures.
     */
    private void onError(CircuitBreakerOnErrorEvent event) {
        String errorType = event.getThrowable().getClass().getSimpleName();
        String errorMessage = event.getThrowable().getMessage();
        long durationMs = event.getElapsedDuration().toMillis();

        log.warn("❌ Nominatim geocoding FAILED - Error: {} - Message: {} - Duration: {}ms",
                errorType, errorMessage, durationMs);

        // Log specific error types for troubleshooting
        if (event.getThrowable() instanceof org.springframework.web.client.ResourceAccessException) {
            log.warn("⏱️ Timeout detected - Nominatim may be slow or unreachable ({}ms)", durationMs);
        } else if (event.getThrowable() instanceof org.springframework.web.client.HttpServerErrorException) {
            log.warn("🌐 HTTP Server Error - Nominatim service may be experiencing issues");
        }
    }

    /**
     * Called on each successful geocoding call.
     * Trace level logging to avoid noise.
     */
    private void onSuccess(CircuitBreakerOnSuccessEvent event) {
        long durationMs = event.getElapsedDuration().toMillis();
        log.trace("✅ Nominatim geocoding SUCCESS - Duration: {}ms", durationMs);

        // Warn if call was slow (but still succeeded)
        if (durationMs > 5000) {
            log.warn("🐌 Slow geocoding call detected: {}ms (threshold: 5000ms)", durationMs);
        }
    }

    /**
     * Called when an error is ignored (not counted as failure).
     * Useful for debugging configuration issues.
     */
    private void onIgnoredError(CircuitBreakerOnIgnoredErrorEvent event) {
        log.debug("⚠️ Nominatim error IGNORED (not counted): {} - {}",
                event.getThrowable().getClass().getSimpleName(),
                event.getThrowable().getMessage());
    }

    /**
     * Called when Circuit Breaker is manually reset via admin API.
     */
    private void onReset(CircuitBreakerOnResetEvent event) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(event.getCircuitBreakerName());
        log.warn("🔄 Nominatim Circuit Breaker RESET manually - Backend: '{}', New state: {}",
                event.getCircuitBreakerName(),
                cb.getState());

        // If reset to CLOSED, trigger retry of pending failed geocoding
        if (cb.getState() == CircuitBreaker.State.CLOSED) {
            log.info("🔄 Triggering retry of pending failed geocoding attempts after reset...");
            geocodingRetryService.retryAllPendingGeocoding();
        }
    }

    /**
     * Called when a geocoding call is rejected because Circuit Breaker is OPEN.
     * This indicates the circuit is protecting the system from cascading failures.
     */
    private void onCallNotPermitted(CircuitBreakerOnCallNotPermittedEvent event) {
        log.warn("🚫 Nominatim geocoding call REJECTED - Circuit is OPEN (failing fast). " +
                "Backend: '{}'", event.getCircuitBreakerName());

        // Note: This is expected behavior when circuit is open
        // Addresses will be queued in failed_geocoding table for later retry
    }
}