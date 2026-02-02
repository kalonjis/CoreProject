package be.steby.CoreProject.bll.domains.lead.exceptions;

/**
 * Exception thrown when inquiry validation fails.
 */
public class LeadValidationException extends LeadDomainException {

    public LeadValidationException(String message) {
        super(message);
    }
}