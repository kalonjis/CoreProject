package be.steby.CoreProject.bll.domains.supportticket.exceptions;

/**
 * Exception thrown when a support ticket request fails domain validation.
 *
 * <p>HTTP Status: 400 (Bad Request)</p>
 */
public class SupportTicketValidationException extends SupportTicketDomainException {

    private static final String ERROR_CODE = "SUPPORT_TICKET_VALIDATION";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public SupportTicketValidationException(String message) {
        super(message, 400);
    }

    public SupportTicketValidationException(String message, Throwable cause) {
        super(message, 400, cause);
    }
}
