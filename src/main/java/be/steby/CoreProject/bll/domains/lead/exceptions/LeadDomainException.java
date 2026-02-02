package be.steby.CoreProject.bll.domains.lead.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for inquiry domain errors.
 */
public class LeadDomainException extends CoreProjectException {

    public LeadDomainException(String message) {
        super(message);
    }

    public LeadDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}