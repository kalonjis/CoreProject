package be.steby.CoreProject.bll.domains.crm.supportticket.exceptions;

import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;

/**
 * Exception thrown when an invalid status transition is attempted on a support ticket.
 *
 * <p>HTTP Status: 422 (Unprocessable Entity)</p>
 */
public class SupportTicketStatusTransitionException extends SupportTicketDomainException {

    private static final String ERROR_CODE = "SUPPORT_TICKET_INVALID_TRANSITION";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public SupportTicketStatusTransitionException(String message) {
        super(message, 422);
    }

    public static SupportTicketStatusTransitionException invalid(
            SupportTicketStatus from, SupportTicketStatus to) {
        return new SupportTicketStatusTransitionException(
                "Invalid status transition for support ticket: " + from + " → " + to);
    }
}
