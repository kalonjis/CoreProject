package be.steby.CoreProject.pl.domains.monitoring.models.responses;

/**
 * Response DTO for SMTP health test.
 *
 * @param reachable      true if SMTP server responded successfully
 * @param responseTimeMs time taken for the test in milliseconds
 * @param errorMessage   error description if failed, null if successful
 */
public record SmtpHealthResponse(
        boolean reachable,
        long responseTimeMs,
        String errorMessage
) {}
