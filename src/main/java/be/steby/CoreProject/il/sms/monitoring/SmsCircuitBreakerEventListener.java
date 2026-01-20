package be.steby.CoreProject.il.sms.monitoring;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listens to Circuit Breaker events for Twilio SMS and logs state transitions.
 *
 * This component provides visibility into Circuit Breaker behavior:
 * - State transitions (CLOSED → OPEN → HALF_OPEN)
 * - Success/failure events
 * - Ignored errors
 *
 * In production, you could extend this to:
 * - Send alerts via Slack/Teams
 * - Push metrics to monitoring systems
 * - Trigger incident management workflows
 *
 * @see be.steby.CoreProject.il.sms.TwilioSmsSender
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsCircuitBreakerEventListener {

    private static final String TWILIO_BACKEND = "twilioBackend";

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerEventListeners() {
        CircuitBreaker twilioCircuitBreaker = circuitBreakerRegistry.circuitBreaker(TWILIO_BACKEND);

        twilioCircuitBreaker.getEventPublisher()
                .onStateTransition(this::onStateTransition)
                .onError(this::onError)
                .onSuccess(this::onSuccess)
                .onIgnoredError(this::onIgnoredError)
                .onReset(this::onReset)
                .onCallNotPermitted(this::onCallNotPermitted);

        log.info("Circuit Breaker event listeners registered for '{}'", TWILIO_BACKEND);
    }

    /**
     * Called when Circuit Breaker changes state.
     * This is the most important event for monitoring.
     */
    private void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.StateTransition transition = event.getStateTransition();

        switch (transition) {
            case CLOSED_TO_OPEN -> {
                log.error("🔴 SMS CIRCUIT BREAKER OPENED - Twilio service is DOWN! " +
                                "Backend: '{}', From: {}, To: {}",
                        event.getCircuitBreakerName(),
                        transition.getFromState(),
                        transition.getToState());
                // TODO: Send alert (Slack, email to admin, PagerDuty, etc.)
                // alertService.sendCriticalAlert("Twilio Circuit Breaker OPENED");
            }
            case OPEN_TO_HALF_OPEN -> {
                log.warn("🟡 SMS CIRCUIT BREAKER HALF-OPEN - Testing Twilio connectivity. " +
                                "Backend: '{}'",
                        event.getCircuitBreakerName());
            }
            case HALF_OPEN_TO_CLOSED -> {
                log.info("🟢 SMS CIRCUIT BREAKER CLOSED - Twilio service recovered! " +
                                "Backend: '{}'",
                        event.getCircuitBreakerName());
                // TODO: Send recovery notification
                // alertService.sendRecoveryNotification("Twilio Circuit Breaker recovered");
            }
            case HALF_OPEN_TO_OPEN -> {
                log.error("🔴 SMS CIRCUIT BREAKER RE-OPENED - Twilio still failing! " +
                                "Backend: '{}'",
                        event.getCircuitBreakerName());
            }
            default -> log.info("SMS Circuit Breaker state transition: {} → {}",
                    transition.getFromState(), transition.getToState());
        }
    }

    /**
     * Called on each failed call (recorded as failure).
     */
    private void onError(CircuitBreakerOnErrorEvent event) {
        log.debug("SMS Circuit Breaker '{}' recorded error: {} (duration: {}ms)",
                event.getCircuitBreakerName(),
                event.getThrowable().getMessage(),
                event.getElapsedDuration().toMillis());
    }

    /**
     * Called on each successful call.
     */
    private void onSuccess(CircuitBreakerOnSuccessEvent event) {
        log.trace("SMS Circuit Breaker '{}' recorded success (duration: {}ms)",
                event.getCircuitBreakerName(),
                event.getElapsedDuration().toMillis());
    }

    /**
     * Called when an error is ignored (not counted as failure).
     */
    private void onIgnoredError(CircuitBreakerOnIgnoredErrorEvent event) {
        log.debug("SMS Circuit Breaker '{}' ignored error: {}",
                event.getCircuitBreakerName(),
                event.getThrowable().getMessage());
    }

    /**
     * Called when Circuit Breaker is manually reset.
     */
    private void onReset(CircuitBreakerOnResetEvent event) {
        log.info("SMS Circuit Breaker '{}' was RESET manually",
                event.getCircuitBreakerName());
    }

    /**
     * Called when a call is rejected because Circuit Breaker is OPEN.
     */
    private void onCallNotPermitted(CircuitBreakerOnCallNotPermittedEvent event) {
        log.warn("SMS Circuit Breaker '{}' rejected call - circuit is OPEN",
                event.getCircuitBreakerName());
    }
}