package be.steby.CoreProject.bll.domains.supportticket.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

/**
 * Base exception for all support ticket domain-related exceptions.
 *
 * <h3>Exception hierarchy</h3>
 * <pre>
 * CoreProjectException
 * └── SupportTicketDomainException
 *     ├── SupportTicketNotFoundException
 *     ├── SupportTicketAlreadyClosedException
 *     ├── SupportTicketStatusTransitionException
 *     ├── SupportTicketAssignNotAuthorizedException
 *     └── SupportTicketRateLimitException
 * </pre>
 */
public abstract class SupportTicketDomainException extends CoreProjectException {

    private static final String ERROR_CODE = "SUPPORT_TICKET_DOMAIN";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public SupportTicketDomainException(String message) {
        super(message, 400);
    }

    public SupportTicketDomainException(String message, int status) {
        super(message, status);
    }

    public SupportTicketDomainException(String message, Throwable cause) {
        super(message, 400, cause);
    }

    public SupportTicketDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}
