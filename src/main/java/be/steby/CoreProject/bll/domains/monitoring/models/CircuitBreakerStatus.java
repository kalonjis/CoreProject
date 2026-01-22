package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * Circuit Breaker status.
 *
 * @param name              Circuit breaker name (e.g., 'smtpBackend')
 * @param state             Current state (CLOSED, OPEN, HALF_OPEN, etc.)
 * @param failureRate       Failure rate percentage (-1 if not enough data)
 * @param slowCallRate      Slow call rate percentage (-1 if not enough data)
 * @param bufferedCalls     Number of calls in sliding window
 * @param failedCalls       Number of failed calls
 * @param successfulCalls   Number of successful calls
 * @param notPermittedCalls Number of calls rejected (circuit open)
 */
public record CircuitBreakerStatus(
    String name,
    String state,
    float failureRate,
    float slowCallRate,
    int bufferedCalls,
    int failedCalls,
    int successfulCalls,
    long notPermittedCalls
) {}