// src/main/java/be/steby/CoreProject/bll/domains/monitoring/models/TwilioHealthResult.java

package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * Result of a Twilio connectivity test.
 *
 * <p>Contains the outcome of a connection test to the Twilio API,
 * including whether the service is reachable, response time, and any error details.</p>
 *
 * <p>This test fetches account info without sending an actual SMS.</p>
 *
 * @param reachable      true if Twilio API responded successfully
 * @param responseTimeMs time taken to complete the test in milliseconds
 * @param accountStatus  Twilio account status (active, suspended, closed) if reachable
 * @param errorMessage   error description if test failed, null otherwise
 */
public record TwilioHealthResult(
        boolean reachable,
        long responseTimeMs,
        String accountStatus,
        String errorMessage
) {
    /**
     * Creates a successful test result.
     *
     * @param responseTimeMs time taken to complete the test
     * @param accountStatus  Twilio account status
     * @return successful TwilioHealthResult
     */
    public static TwilioHealthResult success(long responseTimeMs, String accountStatus) {
        return new TwilioHealthResult(true, responseTimeMs, accountStatus, null);
    }

    /**
     * Creates a failed test result.
     *
     * @param responseTimeMs time taken before failure
     * @param error          description of the failure
     * @return failed TwilioHealthResult
     */
    public static TwilioHealthResult failure(long responseTimeMs, String error) {
        return new TwilioHealthResult(false, responseTimeMs, null, error);
    }
}