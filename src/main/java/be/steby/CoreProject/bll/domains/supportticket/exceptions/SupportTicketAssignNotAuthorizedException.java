package be.steby.CoreProject.bll.domains.supportticket.exceptions;

/**
 * Thrown when a commercial attempts to assign a support ticket to another commercial.
 *
 * <p>Only administrators (ADMIN or SUPER_ADMIN) can assign a ticket to a user
 * other than themselves. A commercial may only self-assign.
 * Maps to HTTP 403.</p>
 */
public class SupportTicketAssignNotAuthorizedException extends SupportTicketDomainException {

    private static final String ERROR_CODE = "SUPPORT_TICKET_ASSIGN_NOT_AUTHORIZED";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public SupportTicketAssignNotAuthorizedException() {
        super("Only administrators can assign a ticket to another commercial", 403);
    }
}
