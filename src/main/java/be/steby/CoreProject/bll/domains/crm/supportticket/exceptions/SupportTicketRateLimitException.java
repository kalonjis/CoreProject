package be.steby.CoreProject.bll.domains.crm.supportticket.exceptions;

/**
 * Thrown when a public support ticket submission exceeds the rate limit for a given IP address.
 */
public class SupportTicketRateLimitException extends SupportTicketDomainException {

    public SupportTicketRateLimitException(String message) {
        super(message, 429);
    }
}
