package be.steby.CoreProject.bll.domains.lead.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

/**
 * Base exception for inquiry domain errors.
 */
public class LeadDomainException extends CoreProjectException {


    private static final String ERROR_CODE = "LEAD_DOMAIN";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public LeadDomainException(String message) {
        super(message);
    }

    public LeadDomainException(String message, int httpStatus) {
        super(message, httpStatus);
    }

    public LeadDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeadDomainException(String message, int httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }
}