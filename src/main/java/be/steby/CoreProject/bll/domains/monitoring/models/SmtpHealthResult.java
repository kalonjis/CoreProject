// src/main/java/be/steby/CoreProject/bll/domains/monitoring/models/SmtpHealthResult.java

package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * Result of an SMTP connectivity test.
 *
 * <p>Contains the outcome of a connection test to the SMTP server,
 * including whether the server is reachable, response time, and any error details.</p>
 *
 * <p>This test performs EHLO/AUTH handshake without sending an actual email.</p>
 *
 * @param reachable      true if SMTP server responded successfully to connection test
 * @param responseTimeMs time taken to complete the test in milliseconds
 * @param errorMessage   error description if test failed, null otherwise
 */
public record SmtpHealthResult(
        boolean reachable,
        long responseTimeMs,
        String errorMessage
) {
    /**
     * Creates a successful test result.
     *
     * @param responseTimeMs time taken to complete the test
     * @return successful SmtpHealthResult
     */
    public static SmtpHealthResult success(long responseTimeMs) {
        return new SmtpHealthResult(true, responseTimeMs, null);
    }

    /**
     * Creates a failed test result.
     *
     * @param responseTimeMs time taken before failure
     * @param error          description of the failure
     * @return failed SmtpHealthResult
     */
    public static SmtpHealthResult failure(long responseTimeMs, String error) {
        return new SmtpHealthResult(false, responseTimeMs, error);
    }
}