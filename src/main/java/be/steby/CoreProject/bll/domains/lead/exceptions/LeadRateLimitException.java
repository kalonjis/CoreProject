package be.steby.CoreProject.bll.domains.lead.exceptions;

/**
 * Exception thrown when inquiry rate limit is exceeded.
 */
public class LeadRateLimitException extends LeadDomainException {

    public LeadRateLimitException(String message) {
        super(message);
    }
}