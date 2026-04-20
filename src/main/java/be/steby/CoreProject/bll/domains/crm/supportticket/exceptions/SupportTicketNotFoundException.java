package be.steby.CoreProject.bll.domains.crm.supportticket.exceptions;

/**
 * Exception thrown when a support ticket cannot be found.
 *
 * <p>HTTP Status: 404 (Not Found)</p>
 */
public class SupportTicketNotFoundException extends SupportTicketDomainException {

    private static final String ERROR_CODE = "SUPPORT_TICKET_NOT_FOUND";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public SupportTicketNotFoundException(String message) {
        super(message, 404);
    }

    public SupportTicketNotFoundException(String message, Throwable cause) {
        super(message, 404, cause);
    }

    public static SupportTicketNotFoundException byId(Long id) {
        return new SupportTicketNotFoundException("Support ticket not found with id: " + id);
    }

    public static SupportTicketNotFoundException byPublicId(String publicId) {
        return new SupportTicketNotFoundException("Support ticket not found with publicId: " + publicId);
    }
}
