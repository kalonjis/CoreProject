package be.steby.CoreProject.pl.domains.monitoring.models.responses;

/**
 * Response DTO for Twilio health test.
 *
 * @param reachable      true if Twilio API responded successfully
 * @param responseTimeMs time taken for the test in milliseconds
 * @param accountStatus  Twilio account status if reachable
 * @param errorMessage   error description if failed, null if successful
 */
public record TwilioHealthResponse(
        boolean reachable,
        long responseTimeMs,
        String accountStatus,
        String errorMessage
) {}