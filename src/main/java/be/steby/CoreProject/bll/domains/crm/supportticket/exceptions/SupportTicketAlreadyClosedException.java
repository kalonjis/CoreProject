package be.steby.CoreProject.bll.domains.crm.supportticket.exceptions;

/**
 * Exception thrown when an operation is attempted on a closed support ticket.
 *
 * <p>HTTP Status: 409 (Conflict)</p>
 */
public class SupportTicketAlreadyClosedException extends SupportTicketDomainException {

    private static final String ERROR_CODE = "SUPPORT_TICKET_ALREADY_CLOSED";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public SupportTicketAlreadyClosedException(String message) {
        super(message, 409);
    }

    public static SupportTicketAlreadyClosedException forTicket(String publicId) {
        return new SupportTicketAlreadyClosedException(
                "Support ticket is already closed and cannot be modified: " + publicId);
    }
}
